package mega.privacy.android.feature.sync.ui.model.exclusion

import androidx.annotation.StringRes
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule

/**
 * A rule as one row of the exclusions list.
 *
 * @property ruleIndex the rule's position among the file's rules, which is the position the edit
 * and remove APIs take, and which tells apart rules that are otherwise identical
 * @property rule the rule as shown, which those same APIs compare against the file to reject a
 * target another client has since moved
 * @property description what the row displays
 */
internal data class SyncExclusionRowUiItem(
    val ruleIndex: Int,
    val rule: SyncExclusionRule,
    val description: SyncExclusionRowDescription,
)

/**
 * What a row shows beneath the pattern on its primary line.
 */
internal sealed interface SyncExclusionRowDescription {

    /**
     * @property supportingTextRes the rule's scope and match mode as a sentence
     * @property value the fragment the rule matches on, which reads correctly only beside
     * [supportingTextRes]
     */
    data class Described(
        @StringRes val supportingTextRes: Int,
        val value: String,
    ) : SyncExclusionRowDescription

    /**
     * A rule the design specified no copy for. It shows [pattern] verbatim, because a fragment
     * without the sentence that frames it says nothing about what the rule does.
     */
    data class Undescribed(val pattern: String) : SyncExclusionRowDescription
}
