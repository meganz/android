package mega.privacy.android.app.activities.navigation

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.link.GetSessionLinkUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import mega.privacy.android.navigation.destination.WebSiteNavKey
import javax.inject.Inject

class WebViewDeepLinkHandler @Inject constructor(
    getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
    private val getSessionLinkUseCase: GetSessionLinkUseCase,
) : AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {

    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
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
        getSessionLinkUseCase(uri.toString())?.let { sessionLink ->
            listOf(WebSiteNavKey(sessionLink))
        } ?: if (regexPatternType == RegexPatternType.MEGA_LINK) {
            listOf(WebSiteNavKey(uri.toString()))
        } else {
            null
        }
    }
}