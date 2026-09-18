package mega.privacy.android.feature.sync.ui.model.exclusion

import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError

/**
 * State of the Manage exclusions screen while the user changes a sync's `.megaignore`.
 *
 * Every edit is staged rather than written. [staged] carries the edits and [loaded] stays as the
 * file was read, which is what lets leaving the screen discard them and lets a visit cost one
 * write and one rescan however many rules were changed. Writing per edit would instead rescan the
 * sync each time, and would make deleting a rule and restoring the defaults destructive enough to
 * need a confirmation each.
 *
 * Both models are absent until the file has been read, and absent together: a mutation applied to
 * a file that was never read would stage a model built from nothing, and saving it would replace
 * the user's real exclusions with it. [isLoading] tells a read still in flight from one that
 * failed, which otherwise look alike from here and call for opposite treatment.
 *
 * @property loaded the exclusions as they were read, which is the baseline [isDirty] compares
 * against and the model a discard returns to
 * @property staged the exclusions as the user has changed them, written as they stand on save
 * @property isWritable whether the sync root would accept a write, read in the same call as the
 * rules so that a read-only root disables saving before the user starts rather than after
 * @property isLoading whether the exclusions are being read
 * @property hasLoadFailed whether the exclusions could not be read. Kept apart from an empty file,
 * because presenting a failed read as "no exclusions" invites the user to save that over a file
 * that may hold rules
 * @property isSaving whether a write is in flight
 * @property editError why the last mutation was refused, for the screen to report. Cleared by
 * [ManageSyncExclusionsViewModel.onEditErrorShown]
 * @property hasSaveFailed whether the last save failed. The edits survive it, so the user can
 * retry without re-entering them
 * @property isDiscardPromptShown whether the user is being asked to confirm losing unsaved edits
 * @property isFinished whether the screen has nothing left to do and should be left
 */
internal data class ManageSyncExclusionsUiState(
    val loaded: ParsedMegaIgnore? = null,
    val staged: ParsedMegaIgnore? = null,
    val isWritable: Boolean = false,
    val isLoading: Boolean = true,
    val hasLoadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val editError: SyncExclusionEditError? = null,
    val hasSaveFailed: Boolean = false,
    val isDiscardPromptShown: Boolean = false,
    val isFinished: Boolean = false,
) {

    /**
     * Whether the staged exclusions differ from the ones that were read.
     *
     * Compared as whole models rather than counted as edits, so that edits cancelling each other
     * out — a rule removed and added back, the defaults restored over a file that already held
     * them — leave nothing to save and no prompt to discard.
     *
     * The comparison is over the file's lines, so it reports a change for an edit that rewrites
     * lines without changing their meaning: re-confirming bounds a file declares in the opposite
     * order stages them in the order this app writes, which is a different file holding the same
     * limits.
     */
    val isDirty: Boolean
        get() = staged != null && staged != loaded

    /**
     * Whether saving is currently possible.
     *
     * A root that cannot be written is excluded here rather than only at the write, so the screen
     * never offers a save it knows will fail.
     */
    val canSave: Boolean
        get() = isDirty && isWritable && !isSaving
}
