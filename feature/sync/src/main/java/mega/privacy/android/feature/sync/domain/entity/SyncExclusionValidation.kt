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
     * An equivalent rule is already present, and the SDK would only ever apply
     * the last of them.
     */
    Duplicate,
}
