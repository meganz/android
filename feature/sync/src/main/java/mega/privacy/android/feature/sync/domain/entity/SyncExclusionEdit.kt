package mega.privacy.android.feature.sync.domain.entity

/**
 * The changes an edit makes to an existing [SyncExclusionRule].
 *
 * A null field leaves the source rule's value alone, so a screen that offers no control for a field
 * cannot express a value for it either — a rule read from a file may carry grammar this app never
 * renders.
 *
 * There is deliberately no way to reset a field to the SDK's default: holding both intentions in
 * one nullable field would make the accidental reset the easier of the two to write.
 *
 * @property pattern the new glob, already trimmed of the trailing whitespace the SDK strips on read
 * @property exclusionClass whether the rule excludes or re-includes
 * @property target which node types the rule applies to. [SyncExclusionTarget.Symlinks] is not
 * accepted, and any value is ignored for a rule that already targets it
 * @property matchType what the pattern is matched against, and how far it inherits
 * @property strategy how the pattern is interpreted
 * @property enabled whether the rule is in effect
 */
data class SyncExclusionEdit(
    val pattern: String? = null,
    val exclusionClass: SyncExclusionClass? = null,
    val target: SyncExclusionTarget? = null,
    val matchType: SyncExclusionMatchType? = null,
    val strategy: SyncMatchStrategy? = null,
    val enabled: Boolean? = null,
)
