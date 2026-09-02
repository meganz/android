package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.ExclusionsSnapshot
import mega.privacy.android.feature.sync.domain.repository.SyncExclusionsRepository
import javax.inject.Inject

/**
 * Reads the exclusions of the sync rooted at a local path, along with whether they can be written
 * back.
 *
 * @param syncExclusionsRepository [SyncExclusionsRepository]
 */
internal class GetSyncExclusionsUseCase @Inject constructor(
    private val syncExclusionsRepository: SyncExclusionsRepository,
) {

    /**
     * Reads the exclusions of the sync rooted at [localRoot].
     *
     * The repository's [Result] is handed on rather than unwrapped, so that a failed read is never
     * conflated with a sync that has no rules: substituting an empty model on failure would show
     * an empty exclusion list for a folder that does have rules, and the next save would wipe
     * them.
     *
     * @param localRoot the sync's local root, already resolved from its folder pair
     */
    suspend operator fun invoke(localRoot: UriPath): Result<ExclusionsSnapshot> =
        syncExclusionsRepository.getExclusions(localRoot)
}
