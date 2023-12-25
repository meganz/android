package mega.privacy.android.domain.usecase.link

import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.repository.RegexRepository
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import java.util.regex.Matcher
import javax.inject.Inject

/**
 * Extract contact link use case
 *
 */
class ExtractContactLinkUseCase @Inject constructor(
    private val regexRepository: RegexRepository,
    private val decodeLinkUseCase: DecodeLinkUseCase,
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase,
) {
    /**
     * Invoke
     *
     * @param link
     * @return contact link
     */
    operator fun invoke(link: String): String? {
        if (link.isNotBlank()) {
            val m: Matcher = regexRepository.webUrlPattern.matcher(link)
            while (m.find()) {
                val url: String = decodeLinkUseCase(m.group())
                if (getUrlRegexPatternTypeUseCase(url) == RegexPatternType.CONTACT_LINK) {
                    return url
                }
            }
        }

        return null
    }
}