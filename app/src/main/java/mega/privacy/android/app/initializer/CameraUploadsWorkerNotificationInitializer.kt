package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.notifications.CameraUploadsNotificationManager
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import timber.log.Timber

/**
 * CameraUploads Worker Notification Initializer
 */
class CameraUploadsWorkerNotificationInitializer : Initializer<Unit> {


    /**
     * Camera Uploads Worker Notification Initializer Entry Point
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CameraUploadsWorkerNotificationInitializerEntryPoint {
        /**
         * MonitorCameraUploadsStatusInfoUseCase
         */

        fun monitorCameraUploadsStatusInfoUseCase(): MonitorCameraUploadsStatusInfoUseCase

        /**
         * App scope
         */
        @ApplicationScope
        fun appScope(): CoroutineScope

        /**
         * CameraUploadsNotificationManager
         */

        fun cameraUploadsNotificationManager(): CameraUploadsNotificationManager
    }

    /**
     * Create
     */
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context,
                CameraUploadsWorkerNotificationInitializerEntryPoint::class.java
            )
        entryPoint.appScope().launch {
            Timber.d("CameraUploadsWorkerNotificationInitializer launched")
            entryPoint.monitorCameraUploadsStatusInfoUseCase().invoke().collect {
                entryPoint.cameraUploadsNotificationManager().showNotification(it)
            }
        }
    }

    /**
     * Dependencies
     */
    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(LoggerInitializer::class.java, WorkManagerInitializer::class.java)
}
