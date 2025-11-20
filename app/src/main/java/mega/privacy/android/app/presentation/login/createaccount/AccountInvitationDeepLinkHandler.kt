package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import javax.inject.Inject

/**
 * Deep link handler for account invitation link
 */
class AccountInvitationDeepLinkHandler @Inject constructor(
    private val querySignupLinkUseCase: QuerySignupLinkUseCase,
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? =
        if (regexPatternType == RegexPatternType.ACCOUNT_INVITATION_LINK) {
            val email = runCatching { querySignupLinkUseCase(uri.toString()) }.getOrNull()

            if (isLoggedIn) {
                snackbarEventQueue.queueMessage(R.string.log_out_warning)
                emptyList()
            } else {
                listOf(CreateAccountNavKey(initialEmail = email))
            }
        } else {
            null
        }
}