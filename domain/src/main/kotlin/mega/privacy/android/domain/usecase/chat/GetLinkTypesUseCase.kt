package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.chat.LinkDetail
import mega.privacy.android.domain.repository.RegexRepository
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import java.util.regex.Matcher
import javax.inject.Inject

/**
 * Get link types use case
 */
class GetLinkTypesUseCase @Inject constructor(
    private val regexRepository: RegexRepository,
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase,
) {
    /**
     * Invoke
     */
    operator fun invoke(content: String): List<LinkDetail> {
        val types = mutableListOf<LinkDetail>()
        val m: Matcher = regexRepository.webUrlPattern.matcher(content)
        while (m.find()) {
            val url: String = m.group().orEmpty()
            types.add(LinkDetail(link = url, type = getUrlRegexPatternTypeUseCase(url)))
        }

        return types
    }
}