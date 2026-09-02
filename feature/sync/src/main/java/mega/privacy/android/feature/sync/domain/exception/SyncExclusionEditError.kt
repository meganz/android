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
}
