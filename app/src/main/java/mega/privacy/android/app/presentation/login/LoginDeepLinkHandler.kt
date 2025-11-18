package mega.privacy.android.app.presentation.login

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler
import javax.inject.Inject

class LoginDeepLinkHandler @Inject constructor() : DeepLinkHandler {
    override suspend fun getNavKeys(
        uri: Uri,
        regexPatternType: RegexPatternType?,
    ): List<NavKey>? =
        if (regexPatternType == RegexPatternType.LOGIN_LINK) {
            listOf(LoginNavKey())
        } else {
            null
        }
}