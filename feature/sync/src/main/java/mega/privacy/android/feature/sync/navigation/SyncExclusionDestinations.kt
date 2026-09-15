package mega.privacy.android.feature.sync.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.ui.exclusion.AddSyncExclusionRuleRoute
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Full-screen destination for authoring a new sync exclusion rule.
 *
 * The destination persists the rule itself before it returns, so nothing about the rule travels
 * back: [RESULT] carries only the confirmation the exclusions list puts in a snackbar, already
 * localised by the destination, and the list rereads the file to pick the rule up.
 *
 * @param syncId id of the sync the rule belongs to, or null while the rule is staged during setup,
 * before a folder pair exists.
 */
@Serializable
data class AddSyncExclusionRuleNavKey(
    val syncId: Long? = null,
) : NavKey {
    companion object {
        /**
         * Result key for the confirmation of the rule that was added.
         *
         * Returning a result pops this destination on its own, so a destination that also removes
         * itself takes the exclusions list down with it. The result then stays put until it is
         * cleared, so the list has to clear it as it shows the confirmation; left in place, it is
         * replayed to the next collector and confirms a rule the user only cancelled.
         */
        const val RESULT = "AddSyncExclusionRuleNavKey::result"
    }
}

/**
 * Full-screen destination for editing or deleting an existing sync exclusion rule.
 *
 * The key carries the chosen rule as well as its position. Every other client of the account
 * writes to the same `.megaignore`, and an insertion above shifts every rule down while leaving
 * each position resolvable, so a destination given only [ruleIndex] would prefill, and then
 * overwrite, whichever rule had taken that place. A moved target is rejected by comparing the rule
 * the user chose against the one now at that position, which is only possible while the chosen
 * rule travels with the position.
 *
 * @param ruleIndex position of the rule among the rules of the sync's `.megaignore`, which tells
 * apart rules that are otherwise identical
 * @param rule the rule as the exclusions list showed it
 * @param syncId id of the sync the rule belongs to, or null while the rule is staged during setup,
 * before a folder pair exists.
 */
@Serializable
data class EditSyncExclusionRuleNavKey(
    val ruleIndex: Int,
    val rule: SyncExclusionRule,
    val syncId: Long? = null,
) : NavKey

/**
 * Registers the exclusion rule destinations.
 *
 * The Add destination builds a rule but does not yet write it, and the Edit destination is still
 * empty. Nothing navigates to either, so no surface can reach a rule that would be dropped.
 */
fun EntryProviderScope<NavKey>.syncExclusionScreens(
    navigationHandler: NavigationHandler,
) {
    entry<AddSyncExclusionRuleNavKey> {
        AddSyncExclusionRuleRoute(
            onConfirm = { navigationHandler.back() },
            onDismiss = { navigationHandler.back() },
        )
    }

    entry<EditSyncExclusionRuleNavKey> {
        Box(modifier = Modifier.fillMaxSize())
    }
}
