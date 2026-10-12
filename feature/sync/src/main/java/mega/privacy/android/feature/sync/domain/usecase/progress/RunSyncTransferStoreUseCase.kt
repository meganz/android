package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStore
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreInput
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import javax.inject.Inject

/**
 * Folds every store command into the sync transfer store and publishes each state, until
 * cancelled. The store's only writer, so exactly one may run at a time. A new generation starts
 * from the reset state, and a command from an older one is dropped.
 */
internal class RunSyncTransferStoreUseCase @Inject constructor(
    private val syncTransferStoreRepository: SyncTransferStoreRepository,
    private val applySyncTransferStoreInputUseCase: ApplySyncTransferStoreInputUseCase,
    @SystemTime private val currentTimeProvider: () -> Long,
) {
    suspend operator fun invoke() {
        var state = SyncTransferStore.reset()
        var generation = 0L
        syncTransferStoreRepository.monitorSyncTransferStoreCommands().collect { command ->
            if (command.generation < generation) return@collect
            if (command.generation > generation) {
                state = SyncTransferStore.reset()
                generation = command.generation
            }
            state = fold(state, command.input) ?: return@collect
            syncTransferStoreRepository.publishSyncTransferStore(state, generation)
        }
    }

    // One failing input must not stop the store for the rest of the session.
    private fun fold(state: SyncTransferStoreState, input: SyncTransferStoreInput): SyncTransferStoreState? =
        runCatching { applySyncTransferStoreInputUseCase(state, input, currentTimeProvider()) }
            .logAndSwallowExceptions()
            .getOrNull()
}
