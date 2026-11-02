package com.example.domain

import com.example.data.local.BlockRuleEntity
import com.example.data.preferences.AegisSettingsState
import java.util.concurrent.ConcurrentHashMap

data class ScreenContextSnapshot(
    val packageName: String,
    val appName: String,
    val windowClassName: String = "",
    val detectedUrl: String = "",
    val visibleTexts: List<String> = emptyList(),
    val viewIdResources: List<String> = emptyList()
)

data class AiContextVerdict(
    val shouldBlock: Boolean,
    val distractionScore: Int, // 0..100
    val thresholdUsed: Int,
    val matchedDistractionSignals: List<String>,
    val matchedProductiveSignals: List<String>,
    val reasoning: String,
    val detectedCategory: String
)

data class InterceptionDecision(
    val shouldBlock: Boolean,
    val triggerType: String = "", // "WEBSITE", "KEYWORD", "INCOGNITO", "REELS_SHORTS", "AI_CONTEXT", "STRICT_TAMPER"
    val matchedRuleLabel: String = "",
    val matchedRuleId: Long? = null,
    val packageName: String = "",
    val appName: String = "",
    val snippet: String = "",
    val aiReasoning: String = "",
    val challengeCodeRequired: String = ""
)

object BlockingEngine {

    val KNOWN_BROWSER_PACKAGES = mapOf(
        "com.android.chrome" to "Google Chrome",
        "com.chrome.beta" to "Chrome Beta",
        "com.chrome.dev" to "Chrome Dev",
        "com.brave.browser" to "Brave Browser",
        "org.mozilla.firefox" to "Mozilla Firefox",
        "org.mozilla.fenix" to "Firefox Nightly",
        "org.mozilla.focus" to "Firefox Focus",
        "com.sec.android.app.sbrowser" to "Samsung Internet",
        "com.microsoft.emmx" to "Microsoft Edge",
        "com.opera.browser" to "Opera Browser",
        "com.opera.mini.native" to "Opera Mini",
        "com.opera.gx" to "Opera GX",
        "com.duckduckgo.mobile.android" to "DuckDuckGo",
        "com.vivaldi.browser" to "Vivaldi",
        "com.kiwibrowser.browser" to "Kiwi Browser",
        "org.torproject.torbrowser" to "Tor Browser",
        "com.mi.globalbrowser" to "Mi Browser",
        "mark.via.gp" to "Via Browser"
    )

    private val KNOWN_URL_BAR_VIEW_IDS = listOf(
        "url_bar",
        "location_bar",
        "mozac_browser_toolbar_url_view",
        "mozac_browser_toolbar_edit_url_view",
        "omnibox_results_container",
        "search_box_text",
        "addressbarEdit",
        "url_field",
        "omnibarTextInput"
    )

    private val INCOGNITO_VIEW_ID_SIGNALS = listOf(
        "incognito",
        "private_browsing",
        "secret_mode",
        "inprivate",
        "private_tab",
        "incognito_badge",
        "private_mode"
    )

    private val INCOGNITO_TEXT_SIGNALS = listOf(
        "you've gone incognito",
        "new incognito tab",
        "incognito mode",
        "close all incognito tabs",
        "inprivate browsing",
        "new inprivate tab",
        "private browsing",
        "new private tab",
        "secret mode",
        "duckplayer private",
        "tor circuit"
    )

    // 5-Minute Temporary Access ("Snooze") Registry: key -> expirationEpochMillis
    private val fiveMinuteSnoozeRegistry = ConcurrentHashMap<String, Long>()

    fun grantFiveMinuteAccess(key: String) {
        val expiresAt = System.currentTimeMillis() + 5 * 60 * 1000L
        fiveMinuteSnoozeRegistry[key.lowercase()] = expiresAt
    }

    fun isCurrentlySnoozed(key: String): Boolean {
        val expiresAt = fiveMinuteSnoozeRegistry[key.lowercase()] ?: return false
        if (System.currentTimeMillis() < expiresAt) {
            return true
        }
        fiveMinuteSnoozeRegistry.remove(key.lowercase())
        return false
    }

    fun getRemainingSnoozeSeconds(key: String): Int {
        val expiresAt = fiveMinuteSnoozeRegistry[key.lowercase()] ?: return 0
        val rem = ((expiresAt - System.currentTimeMillis()) / 1000L).toInt()
        return rem.coerceAtLeast(0)
    }

    fun getActiveSnoozes(): Map<String, Int> {
        val now = System.currentTimeMillis()
        val result = mutableMapOf<String, Int>()
        for ((k, exp) in fiveMinuteSnoozeRegistry.entries) {
            if (exp > now) {
                result[k] = ((exp - now) / 1000L).toInt()
            } else {
                fiveMinuteSnoozeRegistry.remove(k)
            }
        }
        return result
    }

    fun clearAllSnoozes() {
        fiveMinuteSnoozeRegistry.clear()
    }

    fun isBrowserPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return KNOWN_BROWSER_PACKAGES.containsKey(lower) ||
            lower.contains("browser") ||
            lower.contains("chrome") ||
            lower.contains("firefox") ||
            lower.contains("webview")
    }

    fun isKnownUrlBarViewId(viewIdResourceName: String): Boolean {
        val lower = viewIdResourceName.lowercase()
        return KNOWN_URL_BAR_VIEW_IDS.any { lower.contains(it) }
    }

    /**
     * Normalizes text to defeat leetspeak, zero-width spaces, dot-separated keywords (e.g. c.a.s.i.n.o),
     * and repeated characters (e.g. nnnnssssffffwwww).
     */
    fun normalizeObfuscatedText(input: String): String {
        if (input.isBlank()) return ""
        val leetMapped = buildString(input.length) {
            for (ch in input.lowercase()) {
                when (ch) {
                    '0' -> append('o')
                    '1', '!' -> append('i')
                    '3' -> append('e')
                    '4', '@' -> append('a')
                    '5', '$' -> append('s')
                    '7' -> append('t')
                    in 'a'..'z', in '0'..'9', ' ' -> append(ch)
                    '.', '-', '_', '/', ':' -> append(' ')
                    else -> {} // strip zero-width and symbols
                }
            }
        }
        // Also build a collapsed version without spaces between single letters (e.g. "n s f w" -> "nsfw")
        return leetMapped.replace(Regex("\\s+"), " ").trim()
    }

    fun collapseSpacedLetters(input: String): String {
        val leetNormalized = normalizeObfuscatedText(input)
        return leetNormalized
            .replace(Regex("[^a-z0-9]"), "")
            .replace(Regex("(.)\\1{2,}"), "$1") // collapse 3+ repeated letters
    }

    fun normalizeDomainOrUrl(rawUrl: String): String {
        return rawUrl.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("m.")
            .removePrefix("mobile.")
            .trimEnd('/')
    }

    /**
     * Master evaluation pipeline executed on every Accessibility window/content event
     * as well as the in-app Live Algorithm Tester.
     */
    fun evaluateScreenContext(
        snapshot: ScreenContextSnapshot,
        settings: AegisSettingsState,
        enabledRules: List<BlockRuleEntity>
    ): InterceptionDecision {
        val pkg = snapshot.packageName.lowercase()
        val combinedRawText = (snapshot.visibleTexts + snapshot.detectedUrl).joinToString(" ")
        val lowerCombinedText = combinedRawText.lowercase()
        val lowerViewIds = snapshot.viewIdResources.map { it.lowercase() }

        val effectiveChallengeCode = if (settings.useRandomChallengeCode) {
            generateDeterministicSessionCode(snapshot.packageName)
        } else {
            settings.defaultChallengeCode
        }

        // 0. Check if this package or trigger is currently on a 5-minute snooze pass
        if (isCurrentlySnoozed(pkg)) {
            return InterceptionDecision(shouldBlock = false)
        }

        // 1. STRICT MODE ANTI-TAMPER GUARD (Prevents disabling Accessibility or Overlay or Force Stopping Aegis)
        if (settings.isStrictCurrentlyLocked && settings.strictModePreventSettingsTamper) {
            if (pkg == "com.android.settings" || pkg.contains("packageinstaller")) {
                val mentionsAegis = lowerCombinedText.contains("aegis control") ||
                    lowerCombinedText.contains("aegis self-control")
                val mentionsDangerAction = lowerCombinedText.contains("force stop") ||
                    lowerCombinedText.contains("uninstall") ||
                    lowerCombinedText.contains("turn off") ||
                    lowerCombinedText.contains("stop service") ||
                    lowerCombinedText.contains("display over other apps")
                if (mentionsAegis && mentionsDangerAction) {
                    return InterceptionDecision(
                        shouldBlock = true,
                        triggerType = "STRICT_TAMPER",
                        matchedRuleLabel = "Strict Mode Anti-Bypass Protection",
                        packageName = snapshot.packageName,
                        appName = "System Settings",
                        snippet = "Attempted to modify or disable Aegis Control while Strict Lock is active.",
                        challengeCodeRequired = effectiveChallengeCode
                    )
                }
            }
        }

        // 2. INCOGNITO MODE BLOCKER (Across all browsers)
        if (settings.incognitoBlockerEnabled && isBrowserPackage(pkg)) {
            val matchedViewId = lowerViewIds.firstOrNull { id ->
                INCOGNITO_VIEW_ID_SIGNALS.any { sig -> id.contains(sig) }
            }
            val matchedText = INCOGNITO_TEXT_SIGNALS.firstOrNull { sig ->
                lowerCombinedText.contains(sig) || snapshot.windowClassName.lowercase().contains("incognito")
            }
            if (matchedViewId != null || matchedText != null) {
                val reason = matchedText ?: matchedViewId ?: "Incognito surface"
                if (!isCurrentlySnoozed("incognito:$pkg")) {
                    return InterceptionDecision(
                        shouldBlock = true,
                        triggerType = "INCOGNITO",
                        matchedRuleLabel = "Incognito / Private Browsing Blocked",
                        packageName = snapshot.packageName,
                        appName = snapshot.appName,
                        snippet = "Detected private browsing indicator: \"$reason\"",
                        challengeCodeRequired = effectiveChallengeCode
                    )
                }
            }
        }

        // 3. POWERFUL WEBSITE & KEYWORD BLOCKER
        if (settings.websiteKeywordBlockerEnabled) {
            val normalizedUrl = normalizeDomainOrUrl(snapshot.detectedUrl)
            val normalizedScreenText = normalizeObfuscatedText(combinedRawText)
            val collapsedScreenText = collapseSpacedLetters(combinedRawText)

            for (rule in enabledRules) {
                val rulePattern = rule.pattern.trim().lowercase()
                if (rulePattern.isEmpty()) continue
                if (isCurrentlySnoozed("rule:${rule.id}")) continue

                if (rule.ruleType == "WEBSITE") {
                    val cleanTarget = normalizeDomainOrUrl(rulePattern)
                    val urlMatched = normalizedUrl.isNotEmpty() && (
                        normalizedUrl == cleanTarget ||
                            normalizedUrl.startsWith("$cleanTarget/") ||
                            normalizedUrl.contains(".$cleanTarget") ||
                            normalizedUrl.contains(cleanTarget)
                        )
                    // Also check if a browser's visible URL bar text contains the domain directly
                    val browserTextUrlMatched = isBrowserPackage(pkg) &&
                        lowerCombinedText.contains(cleanTarget)

                    if (urlMatched || browserTextUrlMatched) {
                        return InterceptionDecision(
                            shouldBlock = true,
                            triggerType = "WEBSITE",
                            matchedRuleLabel = "Blocked Website: ${rule.pattern}",
                            matchedRuleId = rule.id,
                            packageName = snapshot.packageName,
                            appName = snapshot.appName,
                            snippet = snapshot.detectedUrl.ifBlank {
                                combinedRawText.take(120)
                            },
                            challengeCodeRequired = effectiveChallengeCode
                        )
                    }
                } else {
                    // KEYWORD BLOCKER (Direct + Obfuscation/Fuzzy matching)
                    val directMatch = lowerCombinedText.contains(rulePattern) ||
                        normalizedUrl.contains(rulePattern)
                    val normalizedTarget = normalizeObfuscatedText(rulePattern)
                    val collapsedTarget = collapseSpacedLetters(rulePattern)

                    val fuzzyMatch = settings.aggressiveFuzzyMatching && (
                        (normalizedTarget.length >= 4 && normalizedScreenText.contains(normalizedTarget)) ||
                            (collapsedTarget.length >= 5 && collapsedScreenText.contains(collapsedTarget))
                        )

                    if (directMatch || fuzzyMatch) {
                        return InterceptionDecision(
                            shouldBlock = true,
                            triggerType = "KEYWORD",
                            matchedRuleLabel = "Blocked Keyword: \"${rule.pattern}\" (${rule.category})",
                            matchedRuleId = rule.id,
                            packageName = snapshot.packageName,
                            appName = snapshot.appName,
                            snippet = extractMatchingSnippet(combinedRawText, rulePattern),
                            challengeCodeRequired = effectiveChallengeCode
                        )
                    }
                }
            }
        }

        // 4. REELS & YOUTUBE SHORTS BLOCKER
        val reelsShortDecision = checkReelsAndShorts(
            pkg = pkg,
            appName = snapshot.appName,
            lowerViewIds = lowerViewIds,
            lowerText = lowerCombinedText,
            detectedUrl = snapshot.detectedUrl.lowercase(),
            settings = settings,
            challengeCode = effectiveChallengeCode
        )
        if (reelsShortDecision != null) {
            return reelsShortDecision
        }

        // 5. OPTIONAL AI SCREEN CONTEXT GUARDIAN
        if (settings.aiGuardianEnabled && settings.aiAutoBlockOnVerdict && combinedRawText.length >= 12) {
            val aiVerdict = evaluateAiScreenContext(
                snapshot = snapshot,
                userInstructions = settings.aiUserInstructions,
                threshold = settings.aiSensitivityThreshold
            )
            if (aiVerdict.shouldBlock && !isCurrentlySnoozed("ai:$pkg")) {
                return InterceptionDecision(
                    shouldBlock = true,
                    triggerType = "AI_CONTEXT",
                    matchedRuleLabel = "AI Guardian (${aiVerdict.distractionScore}% Risk • ${aiVerdict.detectedCategory})",
                    packageName = snapshot.packageName,
                    appName = snapshot.appName,
                    snippet = combinedRawText.take(140),
                    aiReasoning = aiVerdict.reasoning,
                    challengeCodeRequired = effectiveChallengeCode
                )
            }
        }

        return InterceptionDecision(shouldBlock = false)
    }

    private fun checkReelsAndShorts(
        pkg: String,
        appName: String,
        lowerViewIds: List<String>,
        lowerText: String,
        detectedUrl: String,
        settings: AegisSettingsState,
        challengeCode: String
    ): InterceptionDecision? {
        // YouTube Shorts
        if (settings.youtubeShortsBlockerEnabled) {
            val isYtAppShorts = pkg.contains("youtube") && (
                lowerViewIds.any {
                    it.contains("reel_recycler") ||
                        it.contains("reel_player_page_container") ||
                        it.contains("shorts_video") ||
                        it.contains("reel_watch_fragment_root")
                } || (lowerText.contains("remix") && lowerText.contains("shorts") && lowerText.contains("subscribe"))
                )
            val isBrowserYtShorts = detectedUrl.contains("youtube.com/shorts") ||
                lowerText.contains("youtube.com/shorts/")

            if ((isYtAppShorts || isBrowserYtShorts) && !isCurrentlySnoozed("shorts:youtube")) {
                return InterceptionDecision(
                    shouldBlock = true,
                    triggerType = "REELS_SHORTS",
                    matchedRuleLabel = "YouTube Shorts Blocker",
                    packageName = pkg,
                    appName = appName,
                    snippet = "YouTube Shorts vertical feed detected and intercepted.",
                    challengeCodeRequired = challengeCode
                )
            }
        }

        // Instagram Reels
        if (settings.instagramReelsBlockerEnabled) {
            val isIgAppReels = pkg.contains("instagram") && (
                lowerViewIds.any {
                    it.contains("clips_viewer_view_pager") ||
                        it.contains("reel_viewer") ||
                        it.contains("clips_video_container")
                } || (lowerText.contains("reels") && lowerText.contains("original audio"))
                )
            val isBrowserIgReels = detectedUrl.contains("instagram.com/reels") ||
                lowerText.contains("instagram.com/reel/")

            if ((isIgAppReels || isBrowserIgReels) && !isCurrentlySnoozed("shorts:instagram")) {
                return InterceptionDecision(
                    shouldBlock = true,
                    triggerType = "REELS_SHORTS",
                    matchedRuleLabel = "Instagram Reels Blocker",
                    packageName = pkg,
                    appName = appName,
                    snippet = "Instagram Reels endless vertical video stream intercepted.",
                    challengeCodeRequired = challengeCode
                )
            }
        }

        // TikTok
        if (settings.tiktokBlockerEnabled) {
            val isTikTokApp = pkg.contains("musically") || pkg.contains("tiktok") || pkg.contains("ugc.trill")
            val isTikTokWeb = detectedUrl.contains("tiktok.com")
            if ((isTikTokApp || isTikTokWeb) && !isCurrentlySnoozed("shorts:tiktok")) {
                return InterceptionDecision(
                    shouldBlock = true,
                    triggerType = "REELS_SHORTS",
                    matchedRuleLabel = "TikTok Short-Form Feed Blocker",
                    packageName = pkg,
                    appName = appName,
                    snippet = "TikTok short-form vertical feed intercepted.",
                    challengeCodeRequired = challengeCode
                )
            }
        }

        // Facebook Reels
        if (settings.facebookReelsBlockerEnabled) {
            val isFbReels = (pkg.contains("facebook.katana") || isBrowserPackage(pkg)) && (
                lowerViewIds.any { it.contains("fb_shorts") || it.contains("reels_viewer") } ||
                    detectedUrl.contains("facebook.com/reel")
                )
            if (isFbReels && !isCurrentlySnoozed("shorts:facebook")) {
                return InterceptionDecision(
                    shouldBlock = true,
                    triggerType = "REELS_SHORTS",
                    matchedRuleLabel = "Facebook Reels Blocker",
                    packageName = pkg,
                    appName = appName,
                    snippet = "Facebook Reels feed intercepted.",
                    challengeCodeRequired = challengeCode
                )
            }
        }

        return null
    }

    /**
     * On-Device Semantic AI Screen Context Evaluator.
     * Analyzes extracted AccessibilityNodeInfo hierarchy text, URL context, and user-defined
     * natural language instructions ("Allow X, Y... Block A, B...") to decide whether to block
     * or allow watching.
     */
    fun evaluateAiScreenContext(
        snapshot: ScreenContextSnapshot,
        userInstructions: String,
        threshold: Int
    ): AiContextVerdict {
        val rawScreen = (snapshot.visibleTexts + snapshot.detectedUrl).joinToString(" ").lowercase()
        val instructionsLower = userInstructions.lowercase()

        // Parse custom terms from user's natural language prompt
        val userBlockTerms = extractCustomTermsAfterMarkers(
            instructionsLower,
            listOf("block ", "avoid ", "restrict ", "forbid ", "stop ", "no ")
        )
        val userAllowTerms = extractCustomTermsAfterMarkers(
            instructionsLower,
            listOf("allow ", "permit ", "keep ", "study ", "focus on ", "productive ")
        )

        val builtInDistractionWeights = mapOf(
            "viral" to 22,
            "gossip" to 28,
            "celebrity" to 24,
            "drama" to 25,
            "prank" to 26,
            "reaction" to 20,
            "casino" to 38,
            "jackpot" to 36,
            "roulette" to 36,
            "slots" to 34,
            "betting" to 34,
            "nsfw" to 42,
            "18+" to 35,
            "uncensored" to 32,
            "onlyfans" to 42,
            "doomscroll" to 30,
            "brainrot" to 34,
            "trending memes" to 26,
            "watch till the end" to 24,
            "fyp" to 24,
            "#shorts" to 28,
            "#reels" to 28,
            "giveaway" to 18,
            "lootbox" to 25,
            "clickbait" to 24
        )

        val builtInProductiveWeights = mapOf(
            "kotlin" to 26,
            "android" to 22,
            "documentation" to 28,
            "algorithm" to 28,
            "tutorial" to 24,
            "lecture" to 28,
            "mathematics" to 28,
            "research" to 24,
            "github" to 26,
            "stackoverflow" to 26,
            "compiler" to 24,
            "architecture" to 22,
            "science" to 22,
            "engineering" to 25,
            "study" to 22,
            "course" to 22,
            "wikipedia" to 20,
            "textbook" to 26
        )

        val matchedDistractions = mutableListOf<String>()
        val matchedProductive = mutableListOf<String>()
        var score = 18 // Baseline neutral screen score

        // 1. Match user's explicit custom block terms (high weight)
        for (term in userBlockTerms) {
            if (term.length >= 3 && rawScreen.contains(term)) {
                score += 34
                matchedDistractions.add("User Rule: \"$term\"")
            }
        }

        // 2. Match user's explicit custom allow terms (strong reduction)
        for (term in userAllowTerms) {
            if (term.length >= 3 && rawScreen.contains(term)) {
                score -= 28
                matchedProductive.add("User Goal: \"$term\"")
            }
        }

        // 3. Match built-in semantic distraction lexicon
        for ((token, weight) in builtInDistractionWeights) {
            if (rawScreen.contains(token)) {
                score += weight
                if (matchedDistractions.size < 6) {
                    matchedDistractions.add(token)
                }
            }
        }

        // 4. Match built-in productive/educational lexicon
        for ((token, weight) in builtInProductiveWeights) {
            if (rawScreen.contains(token)) {
                score -= weight
                if (matchedProductive.size < 6) {
                    matchedProductive.add(token)
                }
            }
        }

        val finalScore = score.coerceIn(4, 99)
        val shouldBlock = finalScore >= threshold

        val category = when {
            matchedDistractions.any { it.contains("casino") || it.contains("betting") || it.contains("jackpot") } -> "Gambling & Wagering"
            matchedDistractions.any { it.contains("nsfw") || it.contains("onlyfans") || it.contains("18+") } -> "Adult / Restricted"
            matchedDistractions.any { it.contains("gossip") || it.contains("drama") || it.contains("viral") } -> "High-Dopamine Entertainment"
            matchedProductive.isNotEmpty() && !shouldBlock -> "Deep Work & Educational"
            shouldBlock -> "Distracting Screen Context"
            else -> "Safe / Productive Context"
        }

        val reasoning = if (shouldBlock) {
            "AI Screen Context Guardian scored screen at $finalScore% distraction risk (threshold: $threshold%). " +
                "Detected signals: ${matchedDistractions.joinToString(", ").ifBlank { "Distracting feed pattern" }}."
        } else {
            val prodNote = if (matchedProductive.isNotEmpty()) {
                "Verified productive signals (${matchedProductive.joinToString(", ")}). "
            } else ""
            "${prodNote}Distraction risk score is $finalScore% (below $threshold% block threshold). Safe to continue watching."
        }

        return AiContextVerdict(
            shouldBlock = shouldBlock,
            distractionScore = finalScore,
            thresholdUsed = threshold,
            matchedDistractionSignals = matchedDistractions.distinct(),
            matchedProductiveSignals = matchedProductive.distinct(),
            reasoning = reasoning,
            detectedCategory = category
        )
    }

    private fun extractCustomTermsAfterMarkers(text: String, markers: List<String>): List<String> {
        val stopWords = setOf(
            "and", "or", "the", "for", "with", "from", "that", "this", "all", "any",
            "allow", "block", "videos", "websites", "apps", "content", "mode", "only"
        )
        val results = mutableSetOf<String>()
        val sentences = text.split('.', ';', '\n')
        for (sentence in sentences) {
            val trimmed = sentence.trim()
            if (markers.any { trimmed.contains(it) }) {
                val words = trimmed
                    .replace(Regex("[^a-z0-9,\\s-]"), " ")
                    .split(',', ' ')
                    .map { it.trim() }
                    .filter { it.length >= 4 && it !in stopWords }
                results.addAll(words.take(12))
            }
        }
        return results.toList()
    }

    private fun extractMatchingSnippet(fullText: String, keyword: String): String {
        val idx = fullText.lowercase().indexOf(keyword.lowercase())
        if (idx == -1) return fullText.take(120)
        val start = (idx - 35).coerceAtLeast(0)
        val end = (idx + keyword.length + 55).coerceAtMost(fullText.length)
        return "..." + fullText.substring(start, end).trim() + "..."
    }

    private fun generateDeterministicSessionCode(seedPkg: String): String {
        val bucket = System.currentTimeMillis() / (5 * 60 * 1000L)
        val hash = (seedPkg.hashCode() xor bucket.toInt()).let { if (it < 0) -it else it }
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = buildString {
            var v = hash + 104729
            repeat(6) {
                append(chars[v % chars.length])
                v /= chars.length
            }
        }
        return "AEGIS-LOCK-$suffix"
    }
}
