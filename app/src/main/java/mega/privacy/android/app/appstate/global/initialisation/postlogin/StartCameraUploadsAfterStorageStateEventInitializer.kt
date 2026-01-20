package mega.privacy.android.app.appstate.global.initialisation.postlogin

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import mega.privacy.android.app.appstate.global.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Initializer that monitors account detail and manages notification topics accordingly.
 */
class StartCameraUploadsAfterStorageStateEventInitializer @Inject constructor(
    private val monitorMyAccountUpdateUseCase: MonitorMyAccountUpdateUseCase,
    private val startCameraUploads: StartCameraUploadUseCase,
) : PostLoginInitialiser(
    action = { context, _ ->
        monitorMyAccountUpdateUseCase()
            .catch { Timber.e(it, "Failed to monitor my account update event") }
            .filter {
                it.storageState == StorageState.Green || it.storageState == StorageState.Orange
            }
            .collect {
                runCatching {
                    startCameraUploads()
                }.onFailure {
                    Timber.e(it, "Failed to start camera uploads after storage state update")
                }
            }
    }
)