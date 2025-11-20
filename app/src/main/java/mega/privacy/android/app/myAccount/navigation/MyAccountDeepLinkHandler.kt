package mega.privacy.android.app.myAccount.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CANCEL_ACCOUNT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.RESET_PASSWORD_LINK
import mega.privacy.android.domain.entity.RegexPatternType.VERIFY_CHANGE_MAIL_LINK
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import mega.privacy.android.navigation.destination.MyAccountNavKey
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

class MyAccountDeepLinkHandler @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? {
        return when (regexPatternType) {
            CANCEL_ACCOUNT_LINK -> if (isLoggedIn) {
                listOf(
                    MyAccountNavKey(
                        action = Constants.ACTION_CANCEL_ACCOUNT,
                        link = uri.toString()
                    )
                )
            } else {
                snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
                emptyList()
            }

            VERIFY_CHANGE_MAIL_LINK -> if (isLoggedIn) {
                listOf(
                    MyAccountNavKey(
                        action = Constants.ACTION_CHANGE_MAIL,
                        link = uri.toString()
                    )
                )
            } else {
                snackbarEventQueue.queueMessage(sharedR.string.general_alert_not_logged_in)
                emptyList()
            }

            RESET_PASSWORD_LINK -> listOf(
                MyAccountNavKey(
                    action = Constants.ACTION_RESET_PASS,
                    link = uri.toString()
                )
            )

            else -> null
        }
    }

}