package mega.privacy.android.domain.usecase.link

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import javax.inject.Inject

/**
 * Use Case to check if a Url matches any regex pattern listed in MEGA
 */
class GetDecodedUrlRegexPatternTypeUseCase @Inject constructor(
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase,
    private val decodeLinkUseCase: DecodeLinkUseCase,
) {
    /**
     * Invoke
     * @param url as String
     * @return The Regex Pattern type as [RegexPatternType]
     * @see [RegexPatternType]
     */
    operator fun invoke(url: String?): RegexPatternType? =
        url?.let { getUrlRegexPatternTypeUseCase(decodeLinkUseCase(url)) }

}