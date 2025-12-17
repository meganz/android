package mega.privacy.android.app.activities.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.link.GetSessionLinkUseCase
import mega.privacy.android.domain.usecase.link.GetSessionLinkUseCase.Companion.requiresSession
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.navigation.destination.DeepLinksAfterFetchNodesDialogNavKey
import mega.privacy.android.navigation.destination.WebSiteNavKey
import mega.privacy.android.shared.resources.R
import timber.log.Timber
import javax.inject.Inject

class WebViewDeepLinkHandler @Inject constructor(
    private val getSessionLinkUseCase: GetSessionLinkUseCase,
    private val rootNodeExistsUseCase: RootNodeExistsUseCase,
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {

    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        RegexPatternType.EMAIL_VERIFY_LINK,
        RegexPatternType.WEB_SESSION_LINK,
        RegexPatternType.BUSINESS_INVITE_LINK,
        RegexPatternType.MEGA_DROP_LINK,
        RegexPatternType.MEGA_FILE_REQUEST_LINK,
        RegexPatternType.REVERT_CHANGE_PASSWORD_LINK,
        RegexPatternType.INSTALLER_DOWNLOAD_LINK,
        RegexPatternType.MEGA_BLOG_LINK,
        RegexPatternType.PURCHASE_LINK,
            -> listOf(WebSiteNavKey(uri.toString()))

        RegexPatternType.MEGA_LINK -> if (uri.toString().requiresSession()) {
            when {
                !isLoggedIn -> {
                    snackbarEventQueue.queueMessage(R.string.general_alert_not_logged_in)
                    emptyList()
                }

                !rootNodeExistsUseCase() -> {
                    listOf(
                        DeepLinksAfterFetchNodesDialogNavKey(
                            deepLink = uri.toString(),
                            regexPatternType = RegexPatternType.MEGA_LINK
                        )
                    )
                }

                else -> runCatching { getSessionLinkUseCase(uri.toString()) }
                    .onFailure { Timber.w(it) }
                    .getOrNull()?.let { sessionLink ->
                        listOf(WebSiteNavKey(sessionLink))
                    } ?: listOf(WebSiteNavKey(uri.toString()))
            }
        } else {
            listOf(WebSiteNavKey(uri.toString()))
        }

        else -> super.getNavKeys(uri, regexPatternType, isLoggedIn)
    }

    /**
     * We want to open in a web view only if there are no other deep link handlers that can handle the link
     */
    override val priority = Int.MAX_VALUE
}