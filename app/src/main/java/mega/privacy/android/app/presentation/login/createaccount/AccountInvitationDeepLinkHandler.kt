package mega.privacy.android.app.presentation.login.createaccount

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import javax.inject.Inject

/**
 * Deep link handler for account invitation link
 */
class AccountInvitationDeepLinkHandler @Inject constructor(
    getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
    private val querySignupLinkUseCase: QuerySignupLinkUseCase,
) : AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {
    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
    ): List<NavKey>? =
        if (regexPatternType == RegexPatternType.ACCOUNT_INVITATION_LINK) {
            val email = runCatching { querySignupLinkUseCase(uri.toString()) }.getOrNull()
            listOf(CreateAccountNavKey(email))
        } else {
            null
        }
}