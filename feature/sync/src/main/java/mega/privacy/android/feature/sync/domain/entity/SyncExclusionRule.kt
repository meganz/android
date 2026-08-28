package mega.privacy.android.feature.sync.domain.entity

/**
 * A single name, subtree or path rule of a `.megaignore` file.
 *
 * The fields mirror the SDK's filter grammar
 * `<class><target><type><strategy>:<pattern>` one for one, so that a rule read
 * from a file can be written back without changing its meaning. The letters
 * themselves are not modelled here; translating to and from them belongs to the
 * data layer.
 *
 * [target] and [strategy] default to the states the SDK assumes when their letter
 * is absent, so a rule built with those defaults means what an unlettered line
 * means. [matchType] deliberately has no default: with no type letter the SDK
 * derives the type from whether the pattern contains a `/`, which no constant can
 * express. [exclusionClass] has no absent state at all, its letter being
 * mandatory, and defaults to [SyncExclusionClass.Exclude] only because that is
 * what this app authors.
 *
 * @property pattern the glob, with its trailing whitespace already trimmed as the
 * SDK trims it on read
 * @property exclusionClass whether the rule excludes or re-includes
 * @property target which node types the rule applies to
 * @property matchType what the pattern is matched against, and how far it inherits
 * @property strategy how the pattern is interpreted
 * @property enabled false for a rule the user has turned off, which is held in the
 * file as a commented out line so the SDK ignores it while the text survives. The
 * marker has to be a `#` as the line's very first byte, because that is the only
 * position the SDK reads it in; placed after any whitespace it makes the line a
 * syntax error instead of a comment
 */
data class SyncExclusionRule(
    val pattern: String,
    val exclusionClass: SyncExclusionClass = SyncExclusionClass.Exclude,
    val target: SyncExclusionTarget = SyncExclusionTarget.All,
    val matchType: SyncExclusionMatchType,
    val strategy: SyncMatchStrategy = SyncMatchStrategy.GlobCaseInsensitive,
    val enabled: Boolean = true,
)

/**
 * Whether a rule takes paths out of the sync or puts them back in.
 */
enum class SyncExclusionClass {
    /**
     * Excludes what the pattern matches. Written as `-`.
     */
    Exclude,

    /**
     * Re-includes what the pattern matches, overriding any earlier exclusion.
     * Written as `+`.
     */
    Include,
}

/**
 * The node types a rule applies to.
 */
enum class SyncExclusionTarget {
    /**
     * Every node type. Written as `a`, and the state the SDK assumes when the
     * letter is absent.
     */
    All,

    /**
     * Directories only. Written as `d`.
     */
    Directories,

    /**
     * Files only. Written as `f`.
     */
    Files,

    /**
     * Symbolic links only. Written as `s`.
     */
    Symlinks,
}

/**
 * What a rule's pattern is matched against, and whether it applies to nested
 * folders as well as the one holding the file.
 */
enum class SyncExclusionMatchType {
    /**
     * Matches a node's name in the folder holding the `.megaignore` only, and is
     * not inherited by nested folders. Written as `N`.
     *
     * This letter can never be omitted: with no type letter the SDK always treats
     * the rule as inherited, so dropping the `N` widens the rule to the subtree.
     */
    LocalName,

    /**
     * Matches a node's name anywhere in the subtree. Written as `n`, and the state
     * the SDK assumes for a pattern with no type letter and no `/`.
     */
    SubtreeName,

    /**
     * Matches a node's path relative to the folder holding the `.megaignore`.
     * Written as `p`, and the state the SDK assumes for a pattern with no type
     * letter that contains a `/`.
     */
    Path,
}

/**
 * How a rule's pattern is interpreted, over the two dimensions the grammar
 * offers: glob against regular expression, and case sensitivity.
 *
 * All four states are modelled so that a rule read from a file keeps the strategy
 * it was written with, and so that no legal line is unrepresentable. While this
 * app cannot author or edit regular expressions, no rule carries a regular
 * expression strategy: such lines are held verbatim instead.
 */
enum class SyncMatchStrategy {
    /**
     * Glob, ignoring case. Written as `g`, and the state the SDK assumes when the
     * letter is absent, so it is always safe to omit.
     */
    GlobCaseInsensitive,

    /**
     * Glob, respecting case. Written as `G`.
     */
    GlobCaseSensitive,

    /**
     * POSIX extended regular expression, ignoring case. Written as `r`.
     */
    RegexCaseInsensitive,

    /**
     * POSIX extended regular expression, respecting case. Written as `R`.
     */
    RegexCaseSensitive,
}
