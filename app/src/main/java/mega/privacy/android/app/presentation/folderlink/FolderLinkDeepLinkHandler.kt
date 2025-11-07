package mega.privacy.android.app.presentation.folderlink

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.link.GetDecodedUrlRegexPatternTypeUseCase
import mega.privacy.android.navigation.contract.deeplinks.AbstractDeepLinkHandlerRegexPatternType
import mega.privacy.android.navigation.destination.LegacyFolderLinkNavKey
import javax.inject.Inject

/**
 * DeepLinkHandler for folder links
 */
class FolderLinkDeepLinkHandler @Inject constructor(
    getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
) : AbstractDeepLinkHandlerRegexPatternType(getDecodedUrlRegexPatternTypeUseCase) {
    override suspend fun getNavKeysFromRegexPatternType(
        regexPatternType: RegexPatternType,
        uri: Uri,
    ): List<NavKey>? {
        return if (regexPatternType == RegexPatternType.FOLDER_LINK) {
            listOf(LegacyFolderLinkNavKey(uri.toString()))
        } else {
            null
        }
    }

}