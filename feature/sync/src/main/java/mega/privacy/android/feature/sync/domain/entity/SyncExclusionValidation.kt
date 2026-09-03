package mega.privacy.android.feature.sync.domain.entity

/**
 * The outcome of validating a user supplied exclusion pattern before it is
 * turned into a [SyncExclusionRule].
 */
enum class SyncExclusionValidation {
    /**
     * The pattern can be used as it is.
     */
    Valid,

    /**
     * The pattern is empty or contains only whitespace.
     */
    Blank,

    /**
     * The pattern contains a line break, which would split it across two lines of
     * the file and change the meaning of both.
     */
    ContainsLineBreak,

    /**
     * The pattern begins or ends with whitespace, which the grammar cannot carry
     * faithfully, and which is dropped or kept depending on the end it sits at.
     *
     * A trailing run is dropped on the next read, so the rule returns as a shorter
     * pattern than the one saved: one that may now be indistinguishable from a
     * shipped default the user does not own, or from a rule the file already holds.
     * A leading run is instead kept as pattern content, giving a rule that asks for
     * a name beginning with spaces, which no node has.
     */
    LeadingOrTrailingWhitespace,

    /**
     * The pattern contains a `:`, the character the grammar uses to separate a
     * rule's letters from its pattern, and for which the grammar offers no quoting.
     * Reading such a pattern back therefore rests on the parser stopping at the
     * first `:` it finds, which nothing states as a contract.
     */
    ContainsPatternSeparator,

    /**
     * The pattern contains a `/` while the rule matches names rather than paths. The
     * SDK matches such a rule against single names, which never contain a `/`, so the
     * rule would be written successfully and then match nothing at all.
     */
    ContainsPathSeparator,

    /**
     * The pattern contains a `*` or a `?` without the user having asked for wildcard
     * matching. The SDK's glob matcher treats both as special and offers no way to
     * escape them, so the intended exact match would silently become a wildcard one.
     */
    ContainsWildcard,

    /**
     * An equivalent rule is already present, and the SDK would only ever apply
     * the last of them.
     */
    Duplicate,
}
