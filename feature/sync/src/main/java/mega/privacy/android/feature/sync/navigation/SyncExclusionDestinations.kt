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
) : NavKey {
    override fun toString() = "ManageSyncExclusionsNavKey(syncId=$syncId)"
}

/**
 * Full-screen destination for authoring a new sync exclusion rule.
 *
 * The destination writes nothing itself: [RESULT] carries the rules or the bounds the user
 * authored, and the exclusions list stages them into the file it holds, to be written on its own
 * save. The rules travel back because the list never rereads the file — a reread would take the
 * file as it was before the edits it is staging.
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
         * Result key for what the user authored.
         *
         * Returning a result pops this destination on its own, so a destination that also removes
         * itself takes the exclusions list down with it. The result then stays put until it is
         * cleared, so the list has to clear it as it stages the change; left in place, it is
         * replayed to the next collector and adds a rule the user only cancelled.
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
) : NavKey {
    override fun toString() = "EditSyncExclusionRuleNavKey(ruleIndex=$ruleIndex, " +
            "matchType=${rule.matchType}, target=${rule.target}, syncId=$syncId)"

    companion object {
        /** Result key for the change, or the deletion, the user asked for. */
        const val RESULT = "EditSyncExclusionRuleNavKey::result"
    }
}

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
) : NavKey {
    companion object {
        /** Result key for the bounds the file should end up declaring, which the trash empties. */
        const val RESULT = "EditSyncSizeExclusionNavKey::result"
    }
}

/**
 * Registers the exclusion destinations.
 *
 * Only the Manage destination writes: its save hands the staged file to the repository and the
 * sync rescans. The Add, Edit and size destinations hand back what the user authored, which the
 * Manage screen stages alongside its own edits, so a single save covers a whole visit however many
 * screens it passed through.
 */
fun EntryProviderScope<NavKey>.syncExclusionScreens(
    navigationHandler: NavigationHandler,
) {
    entry<ManageSyncExclusionsNavKey> { navKey ->
        ManageSyncExclusionsRoute(
            syncId = navKey.syncId,
            localRoot = navKey.localRoot,
            monitorResult = navigationHandler::monitorResult,
            clearResult = navigationHandler::clearResult,
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

    // returning a result pops the destination itself, so none of these also asks to go back
    entry<AddSyncExclusionRuleNavKey> {
        AddSyncExclusionRuleRoute(
            onConfirm = {
                navigationHandler.returnResult(AddSyncExclusionRuleNavKey.RESULT, it)
            },
            onDismiss = { navigationHandler.back() },
        )
    }

    entry<EditSyncExclusionRuleNavKey> { navKey ->
        EditSyncExclusionRuleRoute(
            rule = navKey.rule,
            onDelete = {
                navigationHandler.returnResult(
                    EditSyncExclusionRuleNavKey.RESULT,
                    EditSyncExclusionRuleResult.Deleted(navKey.ruleIndex, navKey.rule),
                )
            },
            onSave = { edit ->
                navigationHandler.returnResult(
                    EditSyncExclusionRuleNavKey.RESULT,
                    EditSyncExclusionRuleResult.Saved(navKey.ruleIndex, navKey.rule, edit),
                )
            },
            onDismiss = { navigationHandler.back() },
        )
    }

    entry<EditSyncSizeExclusionNavKey> { navKey ->
        EditSyncSizeExclusionRoute(
            exclusion = navKey.exclusion,
            onDelete = {
                navigationHandler.returnResult(
                    EditSyncSizeExclusionNavKey.RESULT,
                    EditSyncSizeExclusionResult(SyncSizeExclusion()),
                )
            },
            onSave = { bounds ->
                navigationHandler.returnResult(
                    EditSyncSizeExclusionNavKey.RESULT,
                    EditSyncSizeExclusionResult(bounds),
                )
            },
            onDismiss = { navigationHandler.back() },
        )
    }
}
