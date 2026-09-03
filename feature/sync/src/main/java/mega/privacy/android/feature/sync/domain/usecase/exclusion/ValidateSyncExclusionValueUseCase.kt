package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.feature.sync.domain.entity.SyncExclusionMatchType
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionValidation
import javax.inject.Inject

/**
 * Validates a user-entered exclusion value against `.megaignore` syntax and SDK matching rules
 * before creating a [mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule].
 */
internal class ValidateSyncExclusionValueUseCase @Inject constructor() {

    /**
     * Validates [value] against syntax and matcher constraints and returns the first failure,
     * or [SyncExclusionValidation.Valid].
     *
     * @param value the user-entered value, untrimmed
     * @param matchType the rule's match type
     * @param wildcardsAllowed whether wildcards (`*`, `?`) are permitted
     */
    operator fun invoke(
        value: String,
        matchType: SyncExclusionMatchType,
        wildcardsAllowed: Boolean,
    ): SyncExclusionValidation = when {
        value.all { it in SDK_WHITESPACE } -> SyncExclusionValidation.Blank

        value.any { it in LINE_BREAKS } -> SyncExclusionValidation.ContainsLineBreak

        value.first() in SDK_WHITESPACE || value.last() in SDK_WHITESPACE ->
            SyncExclusionValidation.LeadingOrTrailingWhitespace

        PATTERN_SEPARATOR in value -> SyncExclusionValidation.ContainsPatternSeparator

        matchType != SyncExclusionMatchType.Path && PATH_SEPARATOR in value ->
            SyncExclusionValidation.ContainsPathSeparator

        !wildcardsAllowed && value.any { it in WILDCARDS } ->
            SyncExclusionValidation.ContainsWildcard

        else -> SyncExclusionValidation.Valid
    }

    private companion object {
        /** Whitespace dropped by the SDK using C-locale `isspace`. */
        const val SDK_WHITESPACE = " \t\n\u000B\u000C\r"
        const val LINE_BREAKS = "\n\r"
        const val PATTERN_SEPARATOR = ':'
        const val PATH_SEPARATOR = '/'
        const val WILDCARDS = "*?"
    }
}
