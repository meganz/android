package mega.privacy.android.data.wrapper

import android.content.Context
import android.content.Intent

/**
 * The interface for wrapping static CameraUploadsService methods.
 */
interface CameraUploadServiceWrapper {

    /**
     * Wrapper method that calls CameraUploadsService.newIntent
     */
    fun newIntent(context: Context): Intent
}
