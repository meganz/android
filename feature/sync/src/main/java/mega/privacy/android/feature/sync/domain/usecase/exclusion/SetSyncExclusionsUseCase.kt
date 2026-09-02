package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.repository.SyncExclusionsRepository
import javax.inject.Inject

/**
 * Replaces the exclusions of the sync rooted at a local path.
 *
 * @param syncExclusionsRepository [SyncExclusionsRepository]
 */
internal class SetSyncExclusionsUseCase @Inject constructor(
    private val syncExclusionsRepository: SyncExclusionsRepository,
) {

    /**
     * Replaces the exclusions of the sync rooted at [localRoot].
     *
     * [exclusions] is written through as it stands, without reordering or rebuilding its entries:
     * the SDK evaluates filters last match wins, so changing the order of the lines changes which
     * rule takes effect.
     *
     * @param localRoot the sync's local root, already resolved from its folder pair
     * @param exclusions the exclusions to write, in the line order they are to be written in
     */
    suspend operator fun invoke(
        localRoot: UriPath,
        exclusions: ParsedMegaIgnore,
    ): Result<Unit> = syncExclusionsRepository.setExclusions(localRoot, exclusions)
}
