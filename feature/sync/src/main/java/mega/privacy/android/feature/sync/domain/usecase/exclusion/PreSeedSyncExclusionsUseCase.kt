package mega.privacy.android.feature.sync.domain.usecase.exclusion

import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreEntry
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.repository.SyncExclusionsRepository
import javax.inject.Inject

/**
 * Writes the exclusions a user staged while setting a sync up, before the folder pair exists.
 *
 * @param syncExclusionsRepository [SyncExclusionsRepository]
 */
internal class PreSeedSyncExclusionsUseCase @Inject constructor(
    private val syncExclusionsRepository: SyncExclusionsRepository,
) {

    /**
     * Writes [staged] to the `.megaignore` of [localRoot], seeding the SDK's predefined exclusions
     * if [staged] does not already account for them.
     *
     * The create time counterpart of [SetSyncExclusionsUseCase], which needs a folder pair that
     * does not exist yet and rescans a sync that is not running.
     *
     * The SDK seeds its defaults only into a sync whose ignore file is absent, so a pre-seeded file
     * would suppress them. They go first, leaving everything staged to win under the SDK's last
     * match wins evaluation, and only when [staged] carries no default at all: one carrying any has
     * been through a screen that showed the user all of them, so a default missing from it was
     * deleted and one that no longer compares equal was edited, and writing the pristine default
     * back above either would undo that choice. A [staged] whose every default was deleted cannot
     * be told from one that never saw them, and is seeded.
     *
     * [staged] becomes the whole file, replacing any `.megaignore` already at [localRoot] rather
     * than merging with it.
     *
     * @param localRoot the local root the sync is being created on
     * @param staged the exclusions the user configured during setup, in the line order they are to
     * be written in
     */
    suspend operator fun invoke(
        localRoot: UriPath,
        staged: ParsedMegaIgnore,
    ): Result<Unit> = syncExclusionsRepository.setExclusions(localRoot, staged.seededWithDefaults())

    private fun ParsedMegaIgnore.seededWithDefaults(): ParsedMegaIgnore =
        if (carriesADefault()) {
            this
        } else {
            copy(entries = MegaIgnoreDefaults.RULES.map { MegaIgnoreEntry.RuleEntry(it) } + entries)
        }

    private fun ParsedMegaIgnore.carriesADefault(): Boolean = entries.any {
        it is MegaIgnoreEntry.RuleEntry && MegaIgnoreDefaults.isDefault(it.rule)
    }
}
