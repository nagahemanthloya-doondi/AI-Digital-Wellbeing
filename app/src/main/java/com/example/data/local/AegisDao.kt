package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AegisDao {
    // --- Block Rules ---
    @Query("SELECT * FROM block_rules ORDER BY isEnabled DESC, hitCount DESC, createdAt DESC")
    fun observeAllRules(): Flow<List<BlockRuleEntity>>

    @Query("SELECT * FROM block_rules WHERE isEnabled = 1")
    suspend fun getEnabledRulesSnapshot(): List<BlockRuleEntity>

    @Query("SELECT COUNT(*) FROM block_rules")
    suspend fun getRuleCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: BlockRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<BlockRuleEntity>)

    @Update
    suspend fun updateRule(rule: BlockRuleEntity)

    @Query("DELETE FROM block_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)

    @Query("UPDATE block_rules SET hitCount = hitCount + 1 WHERE id = :ruleId")
    suspend fun incrementRuleHitCount(ruleId: Long)

    // --- Interception Logs ---
    @Query("SELECT * FROM interception_logs ORDER BY timestamp DESC LIMIT 100")
    fun observeRecentLogs(): Flow<List<InterceptionLogEntity>>

    @Query("SELECT COUNT(*) FROM interception_logs")
    fun observeTotalInterceptionsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterceptionLog(log: InterceptionLogEntity)

    @Query("DELETE FROM interception_logs")
    suspend fun clearAllLogs()

    // --- Scroll & Usage Telemetry ---
    @Query("SELECT * FROM scroll_telemetry ORDER BY scrollDurationMillis + foregroundUsageMillis DESC")
    fun observeAllTelemetry(): Flow<List<ScrollTelemetryEntity>>

    @Query("SELECT * FROM scroll_telemetry WHERE packageName = :pkg LIMIT 1")
    suspend fun getTelemetryForPackage(pkg: String): ScrollTelemetryEntity?

    @Query("SELECT COUNT(*) FROM scroll_telemetry")
    suspend fun getTelemetryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTelemetry(telemetry: ScrollTelemetryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTelemetryList(list: List<ScrollTelemetryEntity>)

    @Query("UPDATE scroll_telemetry SET isAvoidMarked = :avoid, category = :category WHERE packageName = :pkg")
    suspend fun setPackageAvoidStatus(pkg: String, avoid: Boolean, category: String)

    // --- Captured Notifications & Summary Digests ---
    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC LIMIT 80")
    fun observeCapturedNotifications(): Flow<List<CapturedNotificationEntity>>

    @Query("SELECT * FROM captured_notifications ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentCapturedNotificationsSnapshot(): List<CapturedNotificationEntity>

    @Query("SELECT COUNT(*) FROM captured_notifications WHERE isSummarized = 0")
    suspend fun getUnsummarizedNotificationCount(): Int

    @Query("SELECT COUNT(*) FROM captured_notifications")
    suspend fun getTotalCapturedNotificationCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapturedNotification(notification: CapturedNotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapturedNotifications(notifications: List<CapturedNotificationEntity>)

    @Query("UPDATE captured_notifications SET isSummarized = 1 WHERE isSummarized = 0")
    suspend fun markAllNotificationsSummarized()

    @Query("DELETE FROM captured_notifications")
    suspend fun clearCapturedNotifications()

    @Query("SELECT * FROM notification_digests ORDER BY timestamp DESC LIMIT 25")
    fun observeNotificationDigests(): Flow<List<NotificationDigestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationDigest(digest: NotificationDigestEntity): Long
}
