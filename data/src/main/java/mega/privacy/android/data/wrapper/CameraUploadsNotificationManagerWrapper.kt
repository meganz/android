package mega.privacy.android.data.wrapper

import androidx.work.ForegroundInfo

/**
 * The interface for providing notification helper methods to Camera Uploads Worker
 */
interface CameraUploadsNotificationManagerWrapper {

    /**
     *  get ForegroundInfo for Camera Uploads Worker
     */
    fun getForegroundInfo(): ForegroundInfo
}
