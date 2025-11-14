package mega.privacy.android.app.myAccount.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.RegexPatternType.CANCEL_ACCOUNT_LINK
import mega.privacy.android.domain.entity.RegexPatternType.RESET_PASSWORD_LINK
import mega.privacy.android.domain.entity.RegexPatternType.VERIFY_CHANGE_MAIL_LINK
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import mega.privacy.android.navigation.destination.MyAccountNavKey
import javax.inject.Inject

class MyAccountDeepLinkHandler @Inject constructor(
    getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
) : AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {
    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
    ): List<NavKey>? {
        return when (regexPatternType) {
            CANCEL_ACCOUNT_LINK -> {
                listOf(
                    MyAccountNavKey(
                        action = Constants.ACTION_CANCEL_ACCOUNT,
                        link = uri.toString()
                    )
                )
            }

            VERIFY_CHANGE_MAIL_LINK -> {
                listOf(
                    MyAccountNavKey(
                        action = Constants.ACTION_CHANGE_MAIL,
                        link = uri.toString()
                    )
                )
            }

            RESET_PASSWORD_LINK -> {
                listOf(
                    MyAccountNavKey(
                        action = Constants.ACTION_RESET_PASS,
                        link = uri.toString()
                    )
                )
            }

            else -> {
                null
            }
        }
    }

}