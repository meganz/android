package mega.privacy.android.navigation.contract.deeplinks

import android.net.Uri
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.usecase.GetDecodedUrlRegexPatternTypeUseCase

/**
 * Helper implementation of [DeepLinkHandlerRegexPatternType]
 */
abstract class AbstractDeepLinkHandlerRegexPatternType(
    private val getDecodedUrlRegexPatternTypeUseCase: GetDecodedUrlRegexPatternTypeUseCase,
) : DeepLinkHandlerRegexPatternType {
    override suspend fun getNavKeysFromUri(uri: Uri): List<NavKey>? =
        getDecodedUrlRegexPatternTypeUseCase(uri.toString())?.let {
            getNavKeysFromRegexPatternType(
                it,
                uri
            )
        }
}