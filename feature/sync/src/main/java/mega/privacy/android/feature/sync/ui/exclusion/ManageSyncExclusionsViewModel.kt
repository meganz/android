package mega.privacy.android.feature.sync.ui.exclusion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.qualifier.ApplicationScope
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
import mega.privacy.android.feature.sync.ui.model.exclusion.ManageSyncExclusionsUiState

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
 * The sync is required to already exist. Exclusions chosen while a folder pair is still being set
 * up have no sync to write to and no id to rescan, and travel instead on the setup screen's own
 * state.
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
) : ViewModel() {

    val uiState: StateFlow<ManageSyncExclusionsUiState>
        field = MutableStateFlow(ManageSyncExclusionsUiState())

    init {
        load()
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

    fun onRuleEdited(ruleIndex: Int, expected: SyncExclusionRule, edit: SyncExclusionEdit) {
        stageResult { replaceSyncExclusionUseCase(it, ruleIndex, expected, edit) }
    }

    fun onRuleRemoved(ruleIndex: Int, expected: SyncExclusionRule) {
        stageResult { removeSyncExclusionUseCase(it, ruleIndex, expected) }
    }

    fun onSizeBoundsChanged(bounds: SyncSizeExclusion) {
        stageResult { setSyncSizeBoundsUseCase(it, bounds) }
    }

    fun onDefaultsRestored() {
        stage { restoreDefaultSyncExclusionsUseCase(it) }
    }

    fun onSaveClicked() {
        val staged = uiState.value.staged ?: return
        if (!uiState.value.canSave) return

        uiState.update { it.copy(isSaving = true, hasSaveFailed = false) }
        // the write is followed by the rescan that makes the running sync read it, and the two
        // only add up to an applied change together, so leaving the screen must not cancel them
        applicationScope.launch {
            val written = setSyncExclusionsUseCase(args.syncId, args.localRoot, staged)
            uiState.update {
                if (written.isSuccess) {
                    // the file now holds what was staged, so that becomes the baseline; taking
                    // the current staged instead would mark an edit made during the write as
                    // already saved
                    it.copy(isSaving = false, loaded = staged)
                } else {
                    it.copy(isSaving = false, hasSaveFailed = true)
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
        uiState.update { it.copy(editError = null) }
    }

    fun onSaveFailureShown() {
        uiState.update { it.copy(hasSaveFailed = false) }
    }

    private fun load() {
        uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            getSyncExclusionsUseCase(args.localRoot).fold(
                onSuccess = { snapshot ->
                    uiState.update {
                        it.copy(
                            loaded = snapshot.exclusions,
                            staged = snapshot.exclusions,
                            isWritable = snapshot.isWritable,
                            isLoading = false,
                            hasLoadFailed = false,
                            editError = null,
                            hasSaveFailed = false,
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

    private fun stage(transform: (ParsedMegaIgnore) -> ParsedMegaIgnore) {
        val current = uiState.value.staged ?: return
        uiState.update { it.copy(staged = transform(current)) }
    }

    private fun stageResult(transform: (ParsedMegaIgnore) -> Result<ParsedMegaIgnore>) {
        val current = uiState.value.staged ?: return
        transform(current)
            .onSuccess { staged -> uiState.update { it.copy(staged = staged) } }
            .onFailure { error ->
                val refusal = error as? SyncExclusionEditError ?: throw error
                uiState.update { it.copy(editError = refusal) }
            }
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
     * @property syncId id of the folder pair. The write rescans it, and an invalid handle would
     * rescan every sync, so a resolved id is required
     * @property localRoot the sync local root, already resolved from its folder pair
     */
    data class Args(
        val syncId: Long,
        val localRoot: UriPath,
    )
}
