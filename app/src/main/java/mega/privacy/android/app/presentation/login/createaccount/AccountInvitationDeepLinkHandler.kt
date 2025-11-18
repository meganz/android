package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import javax.inject.Inject

/**
 * Deep link handler for account invitation link
 */
class AccountInvitationDeepLinkHandler @Inject constructor(
    private val querySignupLinkUseCase: QuerySignupLinkUseCase,
) : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? =
        if (regexPatternType == RegexPatternType.ACCOUNT_INVITATION_LINK) {
            val email = runCatching { querySignupLinkUseCase(uri.toString()) }.getOrNull()
            listOf(CreateAccountNavKey(email))
        } else {
            null
        }
}