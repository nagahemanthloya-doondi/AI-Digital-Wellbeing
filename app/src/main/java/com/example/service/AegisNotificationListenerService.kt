package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.AegisRepository
import com.example.data.preferences.AegisSettingsState
import com.example.domain.NotificationSummaryEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AegisNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: AegisRepository

    @Volatile
    private var cachedSettings: AegisSettingsState = AegisSettingsState()

    private var lastSummaryEpochMs: Long = 0L

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isNotificationListenerConnected.value = true

        repository = AegisRepository.getInstance(applicationContext)
        serviceScope.launch {
            repository.settingsFlow.collectLatest { state ->
                cachedSettings = state
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        // Ignore our own Aegis Summary notification & ongoing foreground service notifications
        if (pkg == applicationContext.packageName || sbn.id == NotificationSummaryEngine.SUMMARY_NOTIFICATION_ID) {
            return
        }
        if (sbn.isOngoing) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        val text = (bigText ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()).orEmpty()

        if (title.isBlank() && text.isBlank()) return

        val appLabel = resolveAppLabel(pkg)
        val priorityBucket = NotificationSummaryEngine.classifyNotificationPriority(pkg, title, text)

        serviceScope.launch {
            repository.insertCapturedNotification(
                packageName = pkg,
                appName = appLabel,
                title = title.ifBlank { appLabel },
                content = text,
                priorityBucket = priorityBucket
            )

            if (cachedSettings.notificationSummaryEnabled) {
                // Optionally dismiss low-value promotional notifications once captured for the batch summary
                if (cachedSettings.suppressPromoNotificationsWhenSummarized && priorityBucket == "PROMO_NOISE") {
                    runCatching { cancelNotification(sbn.key) }
                }

                val now = System.currentTimeMillis()
                val unsummarizedCount = repository.getUnsummarizedNotificationCount()
                if (unsummarizedCount >= 1 && now - lastSummaryEpochMs > 3_500L) {
                    lastSummaryEpochMs = now
                    val recentBatch = repository.getRecentCapturedNotificationsSnapshot()
                    val digest = NotificationSummaryEngine.summarizeAllNotificationsAtOnce(
                        notifications = recentBatch,
                        settings = cachedSettings
                    )
                    repository.saveNotificationDigest(digest)
                    if (cachedSettings.autoPostSummaryToNotificationPanel) {
                        NotificationSummaryEngine.postSummaryToNotificationPanel(
                            context = applicationContext,
                            digest = digest
                        )
                    }
                }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isNotificationListenerConnected.value = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        _isNotificationListenerConnected.value = false
        instance = null
        serviceScope.cancel()
    }

    private fun resolveAppLabel(packageName: String): String {
        return runCatching {
            val pm = applicationContext.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    companion object {
        @Volatile
        private var instance: AegisNotificationListenerService? = null

        private val _isNotificationListenerConnected = MutableStateFlow(false)
        val isNotificationListenerConnected: StateFlow<Boolean> = _isNotificationListenerConnected.asStateFlow()

        /**
         * Pulls all active status bar notifications from the Android OS if NotificationListenerService is connected.
         */
        fun captureActiveStatusBarNotifications(): Int {
            val svc = instance ?: return 0
            val activeList = runCatching { svc.activeNotifications }.getOrNull() ?: return 0
            var count = 0
            for (sbn in activeList) {
                if (sbn.packageName != svc.applicationContext.packageName && !sbn.isOngoing) {
                    svc.onNotificationPosted(sbn)
                    count++
                }
            }
            return count
        }
    }
}
