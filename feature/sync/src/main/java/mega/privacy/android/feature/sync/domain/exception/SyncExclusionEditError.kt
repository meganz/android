package mega.privacy.android.feature.sync.domain.exception

import mega.privacy.android.feature.sync.domain.entity.SyncExclusionTarget

/**
 * Why an edit of a `.megaignore` rule could not be applied.
 *
 * Handed back in a [Result] like the rest of this feature's failures, so a caller can tell a target
 * that has moved from a bug in its own arithmetic and choose between re-reading the file and
 * reporting the conflict.
 */
sealed class SyncExclusionEditError(message: String) : Exception(message) {

    /**
     * The rule the caller meant to change is not the one at the index it supplied.
     *
     * A rule is identified by its position and the file is shared, so another client can insert,
     * reorder or remove lines between the read and the edit. A resolvable index proves nothing on
     * its own: an insertion above shifts every rule down without shortening the list.
     *
     * @property ruleIndex the position the caller supplied
     */
    class StaleRuleTarget(val ruleIndex: Int) : SyncExclusionEditError(
        "No rule at index $ruleIndex, or the rule there is not the one being edited"
    )

    /**
     * The edit would leave the file holding the same rule twice, which a list cannot tell apart:
     * switching one off leaves a rule that reads as inactive and still applies.
     *
     * @property pattern the pattern the file already holds a rule for
     */
    class DuplicateRule(val pattern: String) : SyncExclusionEditError(
        "A rule matching \"$pattern\" is already in the file"
    )

    /**
     * The edit supplies a [SyncExclusionTarget] this app does not author.
     *
     * @property target the target the edit supplied
     */
    class UnsupportedTarget(val target: SyncExclusionTarget) : SyncExclusionEditError(
        "$target is not a target this app can author"
    )

    /**
     * Both size bounds would hold the same value, which costs the file every rule it has.
     *
     * The SDK reads the bounds as a lower and an upper limit and stops reading the file at the
     * first line that makes them equal, so an equal pair does not merely fail to exclude by size:
     * none of the file's rules take effect at all.
     *
     * @property bytes the value both bounds would hold
     */
    class EqualSizeBounds(val bytes: Long) : SyncExclusionEditError(
        "Both size bounds would be $bytes bytes, which makes the SDK abandon the whole file"
    )

    /**
     * A size bound that is not a size any file can be measured against.
     *
     * Zero is refused as well as negative, because the upper bound starts level with a lower bound
     * of zero: an `exclude-larger` of zero is equal bounds on its own, before any second line.
     *
     * @property bytes the bound the edit supplied
     */
    class UnusableSizeBound(val bytes: Long) : SyncExclusionEditError(
        "$bytes is not a size a bound can hold"
    )
}
