package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Initialiser that monitors user updates and establishes camera uploads sync handles when the camera uploads folder changes.
 * This initialiser runs on app start and continuously monitors user updates.
 */
class CameraUploadsSyncHandlesUpdaterInitializer @Inject constructor(
    private val monitorUserUpdates: MonitorUserUpdates,
    private val establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase,
) : PostLoginInitialiser(
    action = { _, _ ->
        monitorUserUpdates()
            .catch { Timber.Forest.w("Exception monitoring user updates: $it") }
            .filter { it == UserChanges.CameraUploadsFolder }
            .collect {
                Timber.Forest.d("The Camera Uploads Sync Handles have been changed in the API Refresh the Sync Handles")
                runCatching { establishCameraUploadsSyncHandlesUseCase() }
                    .onFailure { Timber.Forest.e(it) }
            }
    }
)