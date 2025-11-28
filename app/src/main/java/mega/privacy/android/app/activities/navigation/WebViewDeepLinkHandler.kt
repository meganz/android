package mega.privacy.android.app.activities.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetSessionLinkUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.WebSiteNavKey
import timber.log.Timber
import javax.inject.Inject

class WebViewDeepLinkHandler @Inject constructor(
    private val getSessionLinkUseCase: GetSessionLinkUseCase,
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = if (regexPatternType == RegexPatternType.EMAIL_VERIFY_LINK ||
        regexPatternType == RegexPatternType.WEB_SESSION_LINK ||
        regexPatternType == RegexPatternType.BUSINESS_INVITE_LINK ||
        regexPatternType == RegexPatternType.MEGA_DROP_LINK ||
        regexPatternType == RegexPatternType.MEGA_FILE_REQUEST_LINK ||
        regexPatternType == RegexPatternType.REVERT_CHANGE_PASSWORD_LINK ||
        regexPatternType == RegexPatternType.INSTALLER_DOWNLOAD_LINK ||
        regexPatternType == RegexPatternType.MEGA_BLOG_LINK ||
        regexPatternType == RegexPatternType.PURCHASE_LINK
    ) {
        listOf(WebSiteNavKey(uri.toString()))
    } else {
        runCatching { getSessionLinkUseCase(uri.toString()) }
            .onFailure { Timber.w(it) }.getOrNull()?.let { sessionLink ->
                listOf(WebSiteNavKey(sessionLink))
            } ?: if (regexPatternType == RegexPatternType.MEGA_LINK) {
            listOf(WebSiteNavKey(uri.toString()))
        } else {
            null
        }
    }

    /**
     * We want to open in a web view only if there are no other deep link handlers that can handle the link
     */
    override val priority = Int.MAX_VALUE
}