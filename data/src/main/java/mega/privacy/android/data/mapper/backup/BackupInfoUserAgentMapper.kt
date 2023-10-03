package mega.privacy.android.data.mapper.backup

import mega.privacy.android.domain.entity.backup.BackupInfoUserAgent
import javax.inject.Inject

/**
 * Mapper that returns the appropriate [BackupInfoUserAgent] by searching for specific keywords in
 * the User Agent
 *
 * Here's an example of a User Agent of a Mac Device:
 * MEGAsync/4.9.6.0 (Darwin 23.0.0 arm64) MegaClient/4.25.0/64
 */
internal class BackupInfoUserAgentMapper @Inject constructor() {

    /**
     * Invocation function
     *
     * @param sdkUserAgent A potentially nullable User Agent retrieved from the SDK
     * @return The specific [BackupInfoUserAgent]
     */
    operator fun invoke(sdkUserAgent: String?): BackupInfoUserAgent = when {
        sdkUserAgent.doesKeyWordExists("megaandroid") -> BackupInfoUserAgent.ANDROID
        sdkUserAgent.doesKeyWordExists("megaios") -> BackupInfoUserAgent.IPHONE
        sdkUserAgent.doesKeyWordExists("megasync") -> {
            when {
                sdkUserAgent.doesKeyWordExists(
                    "linux", "freebsd", "netbsd", "openbsd", "sunos", "gentoo"
                ) -> BackupInfoUserAgent.LINUX

                sdkUserAgent.doesKeyWordExists("windows") -> BackupInfoUserAgent.WINDOWS
                sdkUserAgent.doesKeyWordExists("mac", "darwin") -> BackupInfoUserAgent.MAC
                else -> BackupInfoUserAgent.UNKNOWN
            }
        }

        else -> BackupInfoUserAgent.UNKNOWN
    }

    /**
     * Checks if the User Agent from the SDK contains any of the listed Keywords
     *
     * @return true if any of the listed Keywords exist, and false if otherwise
     */
    private fun String?.doesKeyWordExists(vararg keyWords: String): Boolean {
        // Match the exact word in the sentence (e.g. if the keyword is "device", match the word
        // "device" in the sentence and not "devices")
        val pattern =
            keyWords.joinToString(prefix = "(?i)\\b", separator = "\\b|\\b", postfix = "\\b")
        val regex = pattern.toRegex()
        return !isNullOrBlank() && regex.containsMatchIn(this)
    }
}