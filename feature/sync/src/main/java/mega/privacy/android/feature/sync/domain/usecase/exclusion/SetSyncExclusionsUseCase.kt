package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.repository.SyncExclusionsRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.RescanSyncUseCase
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Replaces the exclusions of the sync rooted at a local path.
 *
 * @param syncExclusionsRepository [SyncExclusionsRepository]
 * @param rescanSyncUseCase [RescanSyncUseCase]
 */
internal class SetSyncExclusionsUseCase @Inject constructor(
    private val syncExclusionsRepository: SyncExclusionsRepository,
    private val rescanSyncUseCase: RescanSyncUseCase,
) {

    /**
     * Replaces the exclusions of the sync rooted at [localRoot], then makes the running sync
     * apply them.
     *
     * [exclusions] is written through as it stands, without reordering or rebuilding its entries:
     * the SDK evaluates filters last match wins, so changing the order of the lines changes which
     * rule takes effect.
     *
     * A written .megaignore does not take effect on its own — the running sync keeps using the
     * filters it loaded at start-up until it is told to re-read them. The rescan is therefore
     * part of applying an edit, not a follow-up to it, and a write that cannot be followed by a
     * rescan is reported as a failure rather than as an applied change. The SDK's rescan is
     * fire-and-forget, so success means it was dispatched, not that it has finished.
     *
     * @param syncId the id of the folder pair whose exclusions are being replaced. An invalid
     * handle (-1) would make the SDK rescan every sync, so callers must pass a resolved id.
     * @param localRoot the sync's local root, already resolved from its folder pair
     * @param exclusions the exclusions to write, in the line order they are to be written in
     */
    suspend operator fun invoke(
        syncId: Long,
        localRoot: UriPath,
        exclusions: ParsedMegaIgnore,
    ): Result<Unit> {
        val write = syncExclusionsRepository.setExclusions(localRoot, exclusions)
        if (write.isFailure) return write

        return try {
            Result.success(rescanSyncUseCase(syncId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }
}
