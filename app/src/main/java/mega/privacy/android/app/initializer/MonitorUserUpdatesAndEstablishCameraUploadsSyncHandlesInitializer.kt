package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import timber.log.Timber

/**
 * Initialiser that monitors user updates and establishes camera uploads sync handles when the camera uploads folder changes.
 * This initialiser runs on app start and continuously monitors user updates.
 */
class MonitorUserUpdatesAndEstablishCameraUploadsSyncHandlesInitializer : Initializer<Unit> {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MonitorUserUpdatesAndEstablishCameraUploadsSyncHandlesInitialiserEntryPoint {

        fun monitorUserUpdates(): MonitorUserUpdates

        fun establishCameraUploadsSyncHandlesUseCase(): EstablishCameraUploadsSyncHandlesUseCase

        @ApplicationScope
        fun appScope(): CoroutineScope
    }

    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                MonitorUserUpdatesAndEstablishCameraUploadsSyncHandlesInitialiserEntryPoint::class.java
            )
        entryPoint.appScope().launch {
            Timber.d("MonitorUserUpdatesAndEstablishCameraUploadsSyncHandlesInitialiser launched")
            action(
                entryPoint.monitorUserUpdates(),
                entryPoint.establishCameraUploadsSyncHandlesUseCase()
            )
        }
    }

    internal suspend fun action(
        monitorUserUpdates: MonitorUserUpdates,
        establishCameraUploadsSyncHandlesUseCase: EstablishCameraUploadsSyncHandlesUseCase,
    ) {
        monitorUserUpdates()
            .catch { Timber.w("Exception monitoring user updates: $it") }
            .filter { it == UserChanges.CameraUploadsFolder }
            .collect {
                Timber.d("The Camera Uploads Sync Handles have been changed in the API Refresh the Sync Handles")
                runCatching { establishCameraUploadsSyncHandlesUseCase() }
                    .onFailure { Timber.e(it) }
            }
    }

    /**
     * Dependencies
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(mega.privacy.android.app.initializer.LoggerInitializer::class.java)
}
