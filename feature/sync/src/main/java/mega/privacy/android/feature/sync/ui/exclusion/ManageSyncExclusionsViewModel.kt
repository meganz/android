package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionEdit
import mega.privacy.android.feature.sync.domain.entity.SyncExclusionRule
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.domain.exception.SyncExclusionEditError
import mega.privacy.android.feature.sync.domain.usecase.exclusion.AddSyncExclusionUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.GetSyncExclusionsUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.RemoveSyncExclusionUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.ReplaceSyncExclusionUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.RestoreDefaultSyncExclusionsUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.SetSyncExclusionsUseCase
import mega.privacy.android.feature.sync.domain.usecase.exclusion.SetSyncSizeBoundsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.navigation.AddSyncExclusionResult
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionChipMapper
import mega.privacy.android.feature.sync.ui.mapper.exclusion.SyncExclusionRowMapper
import mega.privacy.android.feature.sync.ui.model.exclusion.ManageExclusionsMode
import mega.privacy.android.feature.sync.ui.model.exclusion.ManageSyncExclusionsUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionChipUiItem
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionRowUiItem

/**
 * Stages the user's changes to a sync's `.megaignore` and writes them once, on save.
 *
 * Holding the whole file is what makes an edit reversible: the transforms below each return a new
 * [ParsedMegaIgnore] rather than touching the file, so leaving the screen without saving costs the
 * user nothing and no single edit needs a confirmation of its own.
 *
 * A save replaces the file with what was staged, so a line another client of the account added
 * after this screen read it is dropped. Reading once and writing once is what the staged design
 * buys the rescan budget with, and it is also why [SyncExclusionEditError.StaleRuleTarget] cannot
 * be raised from here: the transforms compare a target against this screen's own copy, never
 * against the file. A save that refuses when the file has moved under it is not built yet.
 *
 * The screen also serves [ManageExclusionsMode.Review]: configuring exclusions for a folder pair
 * that does not exist yet, while it is still being set up. Review reads the local root's file but
 * cannot write or rescan a sync that has not been created; Save instead hands the staged model back
 * to the setup screen, which holds it until the pair is created.
 */
@HiltViewModel(assistedFactory = ManageSyncExclusionsViewModel.Factory::class)
internal class ManageSyncExclusionsViewModel @AssistedInject constructor(
    @Assisted private val args: Args,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getSyncExclusionsUseCase: GetSyncExclusionsUseCase,
    private val setSyncExclusionsUseCase: SetSyncExclusionsUseCase,
    private val addSyncExclusionUseCase: AddSyncExclusionUseCase,
    private val replaceSyncExclusionUseCase: ReplaceSyncExclusionUseCase,
    private val removeSyncExclusionUseCase: RemoveSyncExclusionUseCase,
    private val restoreDefaultSyncExclusionsUseCase: RestoreDefaultSyncExclusionsUseCase,
    private val setSyncSizeBoundsUseCase: SetSyncSizeBoundsUseCase,
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val chipMapper: SyncExclusionChipMapper,
    private val rowMapper: SyncExclusionRowMapper,
) : ViewModel() {

    /** No sync id means there is no folder pair yet to edit the exclusions of. */
    private val mode: ManageExclusionsMode =
        if (args.syncId == null) ManageExclusionsMode.Review else ManageExclusionsMode.Manage

    val uiState: StateFlow<ManageSyncExclusionsUiState>
        field = MutableStateFlow(ManageSyncExclusionsUiState(mode = mode))

    /**
     * The staged rules split into the two halves the screen draws.
     *
     * Derived from [uiState] rather than written into it, so that every path that stages an edit
     * republishes them without having to remember to. The two mappers partition the file's rules
     * between them, so a rule appears in exactly one.
     */
    val chips: StateFlow<List<SyncExclusionChipUiItem>> = uiState
        .map { state -> state.staged?.let(chipMapper::invoke).orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rows: StateFlow<List<SyncExclusionRowUiItem>> = uiState
        .map { state -> state.staged?.let(rowMapper::invoke).orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        load()
        loadSyncName()
    }

    /**
     * Rereads the file after a read that failed.
     *
     * Only a failed read is retried. Rereading over staged edits would discard them without the
     * prompt that leaving the screen gives, and rereading during a save would take the file as it
     * was before the write.
     */
    fun onRetryClicked() {
        if (!uiState.value.hasLoadFailed) return
        load()
    }

    fun onRuleAdded(rule: SyncExclusionRule) {
        stage { addSyncExclusionUseCase(it, rule) }
    }

    /**
     * Stages what the Add exclusion screen authored, and confirms it only once something has been
     * staged: a refused change or a file that was never read leaves the user nothing to have added.
     *
     * Bounds arrive over the file's own rather than replacing the pair, because the Add screen
     * states one direction at a time and is never prefilled: taking it whole would clear the
     * opposite bound the file already held, which the user never asked for and would not see.
     */
    fun onExclusionAdded(result: AddSyncExclusionResult) {
        val staged = when (result) {
            is AddSyncExclusionResult.Rules -> result.rules
                .map { rule -> stage { addSyncExclusionUseCase(it, rule) } }
                .any { it }

            is AddSyncExclusionResult.SizeBounds ->
                stageResult { setSyncSizeBoundsUseCase(it, it.size.with(result.bounds)) }
        }
        if (staged) {
            uiState.update { it.copy(exclusionAddedEvent = triggered) }
        }
    }

    fun onRuleEdited(ruleIndex: Int, expected: SyncExclusionRule, edit: SyncExclusionEdit) {
        if (stageResult { replaceSyncExclusionUseCase(it, ruleIndex, expected, edit) }) {
            uiState.update { it.copy(exclusionUpdatedEvent = triggered) }
        }
    }

    fun onRuleRemoved(ruleIndex: Int, expected: SyncExclusionRule) {
        stageResult { removeSyncExclusionUseCase(it, ruleIndex, expected) }
    }

    /**
     * The size screen's trash hands back no bounds at all, which is a removal rather than an edit,
     * so it is not confirmed.
     */
    fun onSizeBoundsChanged(bounds: SyncSizeExclusion) {
        val staged = stageResult { setSyncSizeBoundsUseCase(it, bounds) }
        if (staged && !bounds.isUnbounded) {
            uiState.update { it.copy(exclusionUpdatedEvent = triggered) }
        }
    }

    fun onDefaultsRestored() {
        // confirmed even when the file already held the defaults, because the user cannot tell
        // an unchanged list from an action that did nothing
        if (stage { restoreDefaultSyncExclusionsUseCase(it) }) {
            uiState.update { it.copy(defaultsRestoredEvent = triggered) }
        }
    }

    fun onSaveClicked() {
        val staged = uiState.value.staged ?: return
        if (!uiState.value.canSave) return

        when (mode) {
            // nothing is written here: the model travels back to the setup screen, which stages
            // it until there is a folder pair for it to be written against
            ManageExclusionsMode.Review -> {
                // also settles loaded against staged, so canSave goes false and a second tap
                // before the pop lands cannot trigger and pop a second time
                uiState.update {
                    it.copy(reviewSavedEvent = triggered(staged), loaded = staged)
                }
            }

            ManageExclusionsMode.Manage -> {
                val syncId = args.syncId ?: return
                uiState.update { it.copy(isSaving = true, saveFailedEvent = consumed) }
                // the write is followed by the rescan that makes the running sync read it, and
                // the two only add up to an applied change together, so leaving the screen must
                // not cancel them
                applicationScope.launch {
                    val written = setSyncExclusionsUseCase(syncId, args.localRoot, staged)
                    uiState.update {
                        if (written.isSuccess) {
                            // the file now holds what was staged, so that becomes the baseline;
                            // taking the current staged instead would mark an edit made during
                            // the write as already saved
                            it.copy(isSaving = false, loaded = staged)
                        } else {
                            it.copy(isSaving = false, saveFailedEvent = triggered)
                        }
                    }
                }
            }
        }
    }

    /**
     * Leaves the screen, asking first if that would lose edits.
     *
     * A save in flight is left to finish rather than prompted over: it outlives the screen, so the
     * edits are not the user's to lose at that point and offering to discard them would describe
     * the opposite of what happens.
     */
    fun onBackPressed() {
        uiState.update {
            when {
                it.isSaving -> it.copy(isFinished = true)
                it.isDirty -> it.copy(isDiscardPromptShown = true)
                else -> it.copy(isFinished = true)
            }
        }
    }

    fun onDiscardConfirmed() {
        uiState.update {
            if (!it.isDiscardPromptShown) {
                it
            } else {
                it.copy(staged = it.loaded, isDiscardPromptShown = false, isFinished = true)
            }
        }
    }

    fun onDiscardDismissed() {
        uiState.update { it.copy(isDiscardPromptShown = false) }
    }

    fun onEditErrorShown() {
        uiState.update { it.copy(editErrorEvent = consumed()) }
    }

    fun onSaveFailureShown() {
        uiState.update { it.copy(saveFailedEvent = consumed) }
    }

    fun onDefaultsRestoredShown() {
        uiState.update { it.copy(defaultsRestoredEvent = consumed) }
    }

    fun onExclusionAddedShown() {
        uiState.update { it.copy(exclusionAddedEvent = consumed) }
    }

    fun onExclusionUpdatedShown() {
        uiState.update { it.copy(exclusionUpdatedEvent = consumed) }
    }

    fun onReviewSavedShown() {
        uiState.update { it.copy(reviewSavedEvent = consumed()) }
    }

    /**
     * Reads [Args.localRoot], the same read for both modes.
     *
     * [ManageExclusionsMode.Review] reads before staging anything rather than starting from the
     * defaults outright: a folder that already holds a `.megaignore` — left by an earlier sync of
     * it, or authored by another MEGA client — has to be shown to the user first, or Save would
     * silently replace it with a model they were never shown. The defaults stand in only once the
     * read comes back with nothing to seed from, and only in Review mode: they are what the SDK
     * would otherwise have set up on a fresh root. [ManageExclusionsMode.Manage] leaves an empty
     * read as the empty model it is, because there the file is real and reporting it as anything
     * else would misstate it.
     */
    private fun load() {
        uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getSyncExclusionsUseCase(args.localRoot).fold(
                onSuccess = { snapshot ->
                    val nothingToSeedFrom = when (mode) {
                        ManageExclusionsMode.Review -> ParsedMegaIgnore(
                            entries = MegaIgnoreDefaults.RULES.map(MegaIgnoreEntry::RuleEntry),
                        )

                        ManageExclusionsMode.Manage -> snapshot.exclusions
                    }
                    val staged = args.staged
                        ?: snapshot.exclusions.takeIf { it.entries.isNotEmpty() }
                        ?: nothingToSeedFrom
                    uiState.update {
                        it.copy(
                            loaded = staged,
                            staged = staged,
                            isWritable = snapshot.isWritable,
                            isLoading = false,
                            hasLoadFailed = false,
                            editErrorEvent = consumed(),
                            saveFailedEvent = consumed,
                            isDiscardPromptShown = false,
                        )
                    }
                },
                onFailure = {
                    uiState.update {
                        it.copy(
                            loaded = null,
                            staged = null,
                            isLoading = false,
                            hasLoadFailed = true,
                        )
                    }
                },
            )
        }
    }

    /**
     * Reads the sync's name for the subtitle, separately from the exclusions.
     *
     * Kept out of [load] because the two answer different questions and fail independently: the
     * name is decoration, so a lookup that fails leaves the subtitle absent rather than presenting
     * the screen as unreadable, and a retry of a failed read has no reason to fetch it again.
     *
     * Returns immediately in [ManageExclusionsMode.Review]: there is no folder pair yet to name.
     */
    private fun loadSyncName() {
        if (mode == ManageExclusionsMode.Review) return
        viewModelScope.launch {
            val name = runCatching { getFolderPairsUseCase() }.getOrNull()
                ?.firstOrNull { it.id == args.syncId }
                ?.pairName
                ?: return@launch
            uiState.update { it.copy(syncName = name) }
        }
    }

    /** Applies [transform] to the staged model, reporting whether there was one to apply it to. */
    private fun stage(transform: (ParsedMegaIgnore) -> ParsedMegaIgnore): Boolean {
        val current = uiState.value.staged ?: return false
        uiState.update { it.copy(staged = transform(current)) }
        return true
    }

    /**
     * Applies a [transform] that can refuse, reporting whether it went through. A refusal is
     * reported to the user instead of being staged.
     */
    private fun stageResult(
        transform: (ParsedMegaIgnore) -> Result<ParsedMegaIgnore>,
    ): Boolean {
        val current = uiState.value.staged ?: return false
        return transform(current)
            .onSuccess { staged -> uiState.update { it.copy(staged = staged) } }
            .onFailure { error ->
                val refusal = error as? SyncExclusionEditError ?: throw error
                uiState.update { it.copy(editErrorEvent = triggered(refusal)) }
            }
            .isSuccess
    }

    @AssistedFactory
    interface Factory {
        fun create(args: Args): ManageSyncExclusionsViewModel
    }

    /**
     * Which sync the screen is editing the exclusions of.
     *
     * Carried as a class because [UriPath] is a value class, and a factory method taking one is
     * mangled to a JVM name Dagger cannot generate against.
     *
     * @property syncId id of the folder pair, or null when no folder pair exists yet, which puts
     * the screen into [ManageExclusionsMode.Review]. The write rescans a resolved id, and an
     * invalid handle would rescan every sync, so [ManageExclusionsMode.Manage] requires one
     * @property localRoot the sync local root, already resolved from its folder pair
     * @property staged in [ManageExclusionsMode.Review], the model already staged by the setup
     * screen, or null if nothing has been staged yet
     */
    data class Args(
        val syncId: Long?,
        val localRoot: UriPath,
        val staged: ParsedMegaIgnore?,
    )
}

/**
 * These bounds with each of [stated] that is declared put over them, leaving the rest as they are.
 *
 * A caller that states both replaces the pair outright, which is what picking a range does.
 */
private fun SyncSizeExclusion.with(stated: SyncSizeExclusion) = SyncSizeExclusion(
    excludeSmallerThanBytes = stated.excludeSmallerThanBytes ?: excludeSmallerThanBytes,
    excludeLargerThanBytes = stated.excludeLargerThanBytes ?: excludeLargerThanBytes,
)
