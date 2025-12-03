package mega.privacy.android.app.presentation.login

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.utils.Constants.ACTION_CONFIRM
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CONFIRMATION_LINK
import mega.privacy.android.domain.entity.RegexPatternType.LOGIN_LINK
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.snackbar.SnackbarEventQueue
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

class LoginDeepLinkHandler @Inject constructor(
    snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler(snackbarEventQueue) {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? = when (regexPatternType) {
        LOGIN_LINK -> listOf(LoginNavKey())
        CONFIRMATION_LINK ->
            listOf(
                LoginNavKey(
                    action = ACTION_CONFIRM,
                    link = uri.toString(),
                )
            )

        else -> null
    }

    override fun loggedOutRequiredMessage(
        navKeys: List<NavKey>,
        regexPatternType: RegexPatternType?,
    ) = if (regexPatternType == CONFIRMATION_LINK) R.string.open_link_account_confirmation_error
    else null
}