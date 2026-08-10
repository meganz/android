package mega.privacy.android.data.mapper.photos

import mega.privacy.android.domain.entity.media.MediaTimelineFilter.Location
import nz.mega.sdk.MegaNodeScopeFilter
import javax.inject.Inject

/**
 * Two-way mapper between [Location] and the matching
 * [MegaNodeScopeFilter] location Int value.
 */
internal class MediaTimelineLocationIntMapper @Inject constructor() {

    /**
     * Maps a [Location] into the SDK location Int value.
     */
    operator fun invoke(location: Location): Int = when (location) {
        Location.CloudDrive -> MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE
        Location.CloudDriveAndVault -> MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE_AND_VAULT
        Location.CloudDriveVaultAndRubbish -> MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE_VAULT_AND_RUBBISH
    }

    /**
     * Maps an SDK location Int value back into a [Location].
     */
    operator fun invoke(value: Int): Location = when (value) {
        MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE -> Location.CloudDrive
        MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE_AND_VAULT -> Location.CloudDriveAndVault
        MegaNodeScopeFilter.LOCATION_CLOUD_DRIVE_VAULT_AND_RUBBISH -> Location.CloudDriveVaultAndRubbish
        else -> throw IllegalArgumentException("Unknown location value: $value")
    }
}
