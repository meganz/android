package mega.privacy.android.domain.usecase

import javax.inject.Inject

/**
 * Use Case to check if a Url matches any Regex patterns provided
 */
class IsUrlMatchesRegexUseCase @Inject constructor() {
    /**
     * When invoked, this method checks whether the Url passed matches any Regex patterns
     * on the list of Regex pattern
     * @param url as the url to check
     * @param patterns as the array of regex pattern as [String]
     */
    operator fun invoke(url: String?, patterns: Array<String>): Boolean =
        !url.isNullOrEmpty() && patterns.any { pattern -> url.matches(Regex(pattern)) }
}