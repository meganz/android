package mega.privacy.android.feature.sync.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.exclusion.AddSyncExclusionRuleRoute
import mega.privacy.android.feature.sync.ui.exclusion.EditSyncExclusionRuleRoute
import mega.privacy.android.feature.sync.ui.exclusion.EditSyncSizeExclusionRoute
import mega.privacy.android.feature.sync.ui.exclusion.ManageSyncExclusionsRoute
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Full-screen destination listing everything one sync leaves out.
 *
 * @param syncId id of the sync whose exclusions these are. Required, unlike on the Add and Edit
 * keys: those are reachable while a folder pair is still being set up, and this screen is not
 * @param localRoot the sync's local root, which is the folder whose `.megaignore` is read
 */
@Serializable
data class ManageSyncExclusionsNavKey(
    val syncId: Long,
    val localRoot: UriPath,
) : NavKey

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
 * Full-screen destination for editing the size bounds of a sync's `.megaignore`, or clearing
 * both of them through the trash.
 *
 * The bounds travel whole rather than by position, because they are not rules: the SDK applies
 * the last `exclude-smaller` and the last `exclude-larger` line, so a file has at most one bound
 * in each direction and neither has an index an edit could be anchored to.
 *
 * @param exclusion the bounds as the exclusions list showed them
 * @param syncId id of the sync the bounds belong to, or null while they are staged during setup,
 * before a folder pair exists.
 */
@Serializable
data class EditSyncSizeExclusionNavKey(
    val exclusion: SyncSizeExclusion,
    val syncId: Long? = null,
) : NavKey

/**
 * Registers the exclusion destinations.
 *
 * The Manage destination writes: its save hands the staged file to the repository and the sync
 * rescans. The Add, Edit and size destinations still only close, and nothing carries their result
 * back to the Manage screen, so a rule authored there is discarded when the screen closes. The
 * Manage screen reaches them all, so that gap is now reachable by a user rather than merely
 * latent; it is the whole of SAT-2470.
 */
fun EntryProviderScope<NavKey>.syncExclusionScreens(
    navigationHandler: NavigationHandler,
) {
    entry<ManageSyncExclusionsNavKey> { navKey ->
        ManageSyncExclusionsRoute(
            syncId = navKey.syncId,
            localRoot = navKey.localRoot,
            onFinished = { navigationHandler.back() },
            onAddExclusion = { syncId ->
                navigationHandler.navigate(AddSyncExclusionRuleNavKey(syncId))
            },
            onEditRule = { row ->
                navigationHandler.navigate(
                    EditSyncExclusionRuleNavKey(
                        ruleIndex = row.ruleIndex,
                        rule = row.rule,
                        syncId = navKey.syncId,
                    ),
                )
            },
            onEditSize = { exclusion ->
                navigationHandler.navigate(
                    EditSyncSizeExclusionNavKey(exclusion, navKey.syncId),
                )
            },
        )
    }

    entry<AddSyncExclusionRuleNavKey> {
        AddSyncExclusionRuleRoute(
            onConfirm = { navigationHandler.back() },
            onDismiss = { navigationHandler.back() },
        )
    }

    entry<EditSyncExclusionRuleNavKey> { navKey ->
        EditSyncExclusionRuleRoute(
            rule = navKey.rule,
            onDelete = { navigationHandler.back() },
            onSave = { navigationHandler.back() },
            onDismiss = { navigationHandler.back() },
        )
    }

    entry<EditSyncSizeExclusionNavKey> { navKey ->
        EditSyncSizeExclusionRoute(
            exclusion = navKey.exclusion,
            onDelete = { navigationHandler.back() },
            onSave = { navigationHandler.back() },
            onDismiss = { navigationHandler.back() },
        )
    }
}
