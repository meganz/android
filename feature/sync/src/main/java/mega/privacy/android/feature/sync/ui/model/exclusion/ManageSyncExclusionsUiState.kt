package mega.privacy.android.feature.sync.ui.model.exclusion

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
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
 * @property editErrorEvent why the last mutation was refused, for the screen to report as a
 * snackbar. Consumed through [ManageSyncExclusionsViewModel.onEditErrorShown]
 * @property saveFailedEvent that the last save failed. The edits survive it, so the user can
 * retry without re-entering them. Consumed through
 * [ManageSyncExclusionsViewModel.onSaveFailureShown]
 * @property defaultsRestoredEvent that the defaults have just been put back, for the screen to
 * confirm. Consumed through [ManageSyncExclusionsViewModel.onDefaultsRestoredShown]
 * @property exclusionAddedEvent that an exclusion authored on another screen has just been staged,
 * for the screen to confirm. Consumed through
 * [ManageSyncExclusionsViewModel.onExclusionAddedShown]
 * @property exclusionUpdatedEvent that an edit to an existing exclusion has just been staged, for
 * the screen to confirm. A removal is not confirmed, since Cancel still undoes it. Consumed through
 * [ManageSyncExclusionsViewModel.onExclusionUpdatedShown]
 * @property isDiscardPromptShown whether the user is being asked to confirm losing unsaved edits
 * @property isFinished whether the screen has nothing left to do and should be left
 * @property syncName the sync's name, shown under the title so the screen says which folder these
 * exclusions belong to. Absent until it is read, and left absent if it cannot be: a screen without
 * a subtitle is better than one that blocks on a name it only displays
 * @property mode whether this screen is editing an existing sync's file or a model staged for one
 * that does not exist yet
 * @property reviewSavedEvent the model to hand back to the setup screen once Review mode's Save is
 * confirmed. Consumed through [ManageSyncExclusionsViewModel.onReviewSavedShown]
 */
internal data class ManageSyncExclusionsUiState(
    val loaded: ParsedMegaIgnore? = null,
    val staged: ParsedMegaIgnore? = null,
    val syncName: String? = null,
    val isWritable: Boolean = false,
    val isLoading: Boolean = true,
    val hasLoadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val editErrorEvent: StateEventWithContent<SyncExclusionEditError> = consumed(),
    val saveFailedEvent: StateEvent = consumed,
    val defaultsRestoredEvent: StateEvent = consumed,
    val exclusionAddedEvent: StateEvent = consumed,
    val exclusionUpdatedEvent: StateEvent = consumed,
    val isDiscardPromptShown: Boolean = false,
    val isFinished: Boolean = false,
    val mode: ManageExclusionsMode = ManageExclusionsMode.Manage,
    val reviewSavedEvent: StateEventWithContent<ParsedMegaIgnore> = consumed(),
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

    /**
     * Whether an exclusion can be authored.
     *
     * A file that was never read has nothing to stage a new rule onto, so one authored then would
     * be dropped without the user being told, having taken them through a whole screen.
     */
    val canAddExclusion: Boolean
        get() = staged != null

    /**
     * Whether putting the exclusions back to the defaults is currently possible.
     *
     * A file that was never read has nothing to reset; one the sync refuses would go on
     * refusing it, because restoring keeps the line it rejects; and a root that cannot be
     * written can never save the result. Each would confirm a reset the user cannot see, or
     * one the sync will never apply.
     */
    val canRestoreDefaults: Boolean
        get() = staged != null && !isLoading && !hasLoadFailed && !isRejected && isWritable

    /**
     * Whether the staged file bounds the size of what it syncs.
     *
     * Read apart from the rules because a file holding only size bounds still has exclusions, and
     * showing it the empty state would invite the user to add a first exclusion over one that is
     * already there.
     */
    val hasSizeBounds: Boolean
        get() = staged?.size?.isUnbounded == false

    /**
     * Whether the sync refuses the staged file, in which case it enforces none of it.
     *
     * Drawn instead of the rules rather than beside them, because listing rules the sync is not
     * applying reports the opposite of what is happening to the user's files.
     */
    val isRejected: Boolean
        get() = staged?.isRejectedBySdk == true

    /**
     * Whether the file excludes something the screen cannot draw.
     *
     * Only the rules and the size bounds are rendered, so a file whose exclusions are all regex
     * rules or re-includes would otherwise be reported as excluding nothing. Comments and the
     * `sync` directives are preserved too but exclude nothing, so they are not counted; a line is
     * taken for a rule when it is neither blank nor a comment, which over-reports a preserved
     * directive and under-reports nothing.
     */
    val hasUnshownRules: Boolean
        get() = staged?.preservedLines.orEmpty().any {
            val line = it.trim()
            line.isNotEmpty() && !line.startsWith(COMMENT_PREFIX)
        }

    /**
     * Whether there is nothing for the list to draw, whatever the file may also hold.
     *
     * Read from the staged model rather than from the rendered lists, so that it cannot disagree
     * with them on a frame where one has been recomputed and the other has not.
     */
    val hasNothingToShow: Boolean
        get() = staged?.rules.isNullOrEmpty() && !hasSizeBounds

    private companion object {
        const val COMMENT_PREFIX = "#"
    }
}
