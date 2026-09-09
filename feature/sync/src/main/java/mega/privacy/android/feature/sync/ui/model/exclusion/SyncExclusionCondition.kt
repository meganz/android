package mega.privacy.android.feature.sync.ui.model.exclusion

/**
 * How a stored glob pattern reads to the user.
 *
 * [allowsWildcards] is held here rather than decided per form so that the Add and Edit screens
 * cannot disagree on what they let through: a wildcard typed into one of the fragment conditions
 * composes a pattern that reads back as [MatchingWildcard], losing the choice the user made.
 */
internal enum class SyncExclusionCondition(val allowsWildcards: Boolean) {
    BeginningWith(allowsWildcards = false),
    Containing(allowsWildcards = false),
    EndingWith(allowsWildcards = false),
    MatchingExactly(allowsWildcards = false),
    MatchingWildcard(allowsWildcards = true),
}

/**
 * How a stored pattern is shown. A pattern is one of these and never both, so no surface can render
 * the same rule twice.
 */
internal sealed interface SyncExclusionDisplay {

    /** A pattern the design shows as a chip carrying only [extension]. */
    data class Extension(val extension: String) : SyncExclusionDisplay

    /** A pattern the design shows as a row reading [condition] beside [value]. */
    data class Condition(
        val condition: SyncExclusionCondition,
        val value: String,
    ) : SyncExclusionDisplay
}
