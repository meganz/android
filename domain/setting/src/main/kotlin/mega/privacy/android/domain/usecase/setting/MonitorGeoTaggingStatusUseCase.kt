package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Monitor Geo Tagging Status Use Case
 */
class MonitorGeoTaggingStatusUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: PermissionRepository,
) {
    /**
     * Invoke
     */
    operator fun invoke(): Flow<Boolean> =
        settingsRepository.monitorGeoTaggingStatus().map { enabled ->
            enabled == true && permissionRepository.isLocationPermissionGranted()
        }
}