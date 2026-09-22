package mega.privacy.android.feature.sync.navigation

import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion

/**
 * What the Add exclusion screen hands back.
 *
 * The two tabs author different things and the exclusions list stages each through a different
 * transform, so they arrive apart rather than as one type told apart by an empty field.
 */
internal sealed interface AddSyncExclusionResult {

    /** Rules to put into the file, one per extension when the extension form authored them. */
    data class Rules(val rules: List<SyncExclusionRule>) : AddSyncExclusionResult

    /** Bounds to declare, which replace the file's pair rather than being added to it. */
    data class SizeBounds(val bounds: SyncSizeExclusion) : AddSyncExclusionResult
}

/**
 * What the Edit exclusion screen hands back.
 *
 * Both outcomes carry the position and the rule the user chose, because the list applies them
 * against its own staged file and has to refuse a target another client has moved.
 */
internal sealed interface EditSyncExclusionRuleResult {

    val ruleIndex: Int
    val expected: SyncExclusionRule

    /** The rule stays, changed by [edit]. */
    data class Saved(
        override val ruleIndex: Int,
        override val expected: SyncExclusionRule,
        val edit: SyncExclusionEdit,
    ) : EditSyncExclusionRuleResult

    /** The rule goes. */
    data class Deleted(
        override val ruleIndex: Int,
        override val expected: SyncExclusionRule,
    ) : EditSyncExclusionRuleResult
}

/**
 * What the Edit size exclusion screen hands back.
 *
 * Clearing through the trash is [bounds] with neither limit rather than a second result: the pair
 * is replaced whole, so an unbounded pair is how the file is told to declare none.
 */
internal data class EditSyncSizeExclusionResult(val bounds: SyncSizeExclusion)
