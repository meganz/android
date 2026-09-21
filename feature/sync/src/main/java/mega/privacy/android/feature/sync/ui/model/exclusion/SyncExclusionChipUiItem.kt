package mega.privacy.android.feature.sync.ui.model.exclusion

import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule

/**
 * An extension rule as one chip of the exclusions list.
 *
 * @property ruleIndex the rule's position among the file's rules, which is the position the remove
 * API takes, and which tells apart rules that are otherwise identical
 * @property rule the rule as shown, which that same API compares against the file to reject a
 * target another client has since moved
 * @property extension the extension without the wildcard that matches it, as the chip labels it
 */
internal data class SyncExclusionChipUiItem(
    val ruleIndex: Int,
    val rule: SyncExclusionRule,
    val extension: String,
)
