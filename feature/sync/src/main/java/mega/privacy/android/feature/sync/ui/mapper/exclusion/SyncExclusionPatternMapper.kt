package mega.privacy.android.feature.sync.ui.mapper.exclusion

import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionDisplay
import javax.inject.Inject

/**
 * Moves a stored pattern between its glob form and the form shown by the list, the extension chips
 * and the Edit screen.
 *
 * Shared rather than per screen because a pattern derived one way on one surface and another way on
 * the next leaves a rule the user cannot reconcile with what is actually excluded.
 */
internal class SyncExclusionPatternMapper @Inject constructor() {

    /**
     * How [pattern] is shown. The chip shape is tried first, so the caller cannot render one rule
     * as both a chip and a row. A pattern no condition describes without dropping characters comes
     * back as [SyncExclusionCondition.MatchingWildcard] carrying the whole pattern.
     */
    operator fun invoke(pattern: String): SyncExclusionDisplay {
        extensionOrNull(pattern)?.let { return SyncExclusionDisplay.Extension(it) }

        val leadingStar = pattern.startsWith(ANY)
        val trailingStar = pattern.length > 1 && pattern.endsWith(ANY)
        val value = pattern.substring(
            if (leadingStar) 1 else 0,
            pattern.length - if (trailingStar) 1 else 0,
        )
        if (value.isEmpty() || value.any { it in WILDCARDS }) {
            return SyncExclusionDisplay.Condition(
                SyncExclusionCondition.MatchingWildcard,
                pattern,
            )
        }
        val condition = when {
            leadingStar && trailingStar -> SyncExclusionCondition.Containing
            leadingStar -> SyncExclusionCondition.EndingWith
            trailingStar -> SyncExclusionCondition.BeginningWith
            else -> SyncExclusionCondition.MatchingExactly
        }
        return SyncExclusionDisplay.Condition(condition, value)
    }

    /**
     * The pattern to store for a [condition] the user picked and the [value] they typed. The value
     * is taken verbatim: rejecting whitespace and stray wildcards belongs to the validator, and the
     * SDK strips a narrower set of characters than Kotlin's own trim would.
     */
    operator fun invoke(condition: SyncExclusionCondition, value: String): String =
        when (condition) {
            SyncExclusionCondition.BeginningWith -> "$value$ANY"
            SyncExclusionCondition.Containing -> "$ANY$value$ANY"
            SyncExclusionCondition.EndingWith -> "$ANY$value"
            SyncExclusionCondition.MatchingExactly,
            SyncExclusionCondition.MatchingWildcard,
                -> value
        }

    /**
     * The pattern to store for a typed [extension], or null when no chip could carry it back. A
     * leading dot is dropped so that "mp3" and ".mp3" cannot survive as two rules, since
     * de-duplication compares patterns exactly. Inner dots stay, so "tar.gz" keeps working.
     */
    fun extensionPattern(extension: String): String? =
        extension.removePrefix(DOT)
            .takeIf { it.isUsableExtension() }
            ?.let { EXTENSION_PREFIX + it }

    private fun extensionOrNull(pattern: String): String? = pattern
        .takeIf { it.startsWith(EXTENSION_PREFIX) }
        ?.removePrefix(EXTENSION_PREFIX)
        ?.takeIf { it.isUsableExtension() }

    /**
     * A further leading dot would be stripped on the way back out, rebuilding a broader pattern than
     * the one stored, so such a pattern is left to the conditions instead.
     */
    private fun String.isUsableExtension(): Boolean = isNotEmpty() && !startsWith(DOT)

    private companion object {
        const val ANY = "*"
        const val DOT = "."
        const val EXTENSION_PREFIX = "$ANY$DOT"
        val WILDCARDS = charArrayOf('*', '?')
    }
}
