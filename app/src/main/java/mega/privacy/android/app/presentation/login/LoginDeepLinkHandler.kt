package mega.privacy.android.app.presentation.login

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.ACTION_CONFIRM
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CONFIRMATION_LINK
import mega.privacy.android.domain.entity.RegexPatternType.LOGIN_LINK
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import mega.privacy.android.navigation.contract.queue.SnackbarEventQueue
import javax.inject.Inject

class LoginDeepLinkHandler @Inject constructor(
    private val snackbarEventQueue: SnackbarEventQueue,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
        isLoggedIn: Boolean,
    ): List<NavKey>? = when (regexPatternType) {
        LOGIN_LINK -> listOf(LoginNavKey())
        CONFIRMATION_LINK ->
            if (isLoggedIn) {
                // TODO: Replace with new AND string once created
                snackbarEventQueue.queueMessage(R.string.log_out_warning)
                emptyList()
            } else {
                listOf(
                    LoginNavKey(
                        action = ACTION_CONFIRM,
                        link = uri.toString(),
                    )
                )
            }

        else -> null
    }
}