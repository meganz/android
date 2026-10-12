package mega.privacy.android.feature.sync.domain.usecase.progress

import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.qualifier.SystemTime
import mega.privacy.android.feature.sync.domain.entity.progress.SyncProgressEpisode
import mega.privacy.android.feature.sync.domain.entity.progress.SyncRunSnapshot
import mega.privacy.android.feature.sync.domain.entity.progress.SyncWorkState
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import javax.inject.Inject

/**
 * Folds every sync update into the run's progress episode and publishes each run snapshot, until
 * cancelled. The snapshot's only writer, so exactly one may run at a time. A new generation starts
 * a new episode, and an update from an older one is dropped.
 */
internal class RunSyncRunSnapshotUseCase @Inject constructor(
    private val syncProgressRepository: SyncProgressRepository,
    private val advanceSyncProgressEpisodeUseCase: AdvanceSyncProgressEpisodeUseCase,
    @SystemTime private val currentTimeProvider: () -> Long,
) {
    suspend operator fun invoke() {
        var episode = SyncProgressEpisode.Initial
        var generation = 0L
        syncProgressRepository.monitorSyncWorkStates().collect { update ->
            if (update.generation < generation) return@collect
            if (update.generation > generation) {
                episode = SyncProgressEpisode.Initial
                generation = update.generation
            }
            episode = advance(episode, update.syncs) ?: return@collect
            val snapshot = SyncRunSnapshot(
                isSeeded = true,
                syncs = update.syncs.sortedBy { it.syncId },
                progress = episode.toProgress(),
            )
            syncProgressRepository.publishSyncRunSnapshot(snapshot, generation)
        }
    }

    // One failing update must not stop the run snapshot for the rest of the session.
    private fun advance(episode: SyncProgressEpisode, syncs: List<SyncWorkState>): SyncProgressEpisode? =
        runCatching { advanceSyncProgressEpisodeUseCase(episode, syncs, currentTimeProvider()) }
            .logAndSwallowExceptions()
            .getOrNull()
}
