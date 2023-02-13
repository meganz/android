package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.jobservices.CameraUploadsService
import mega.privacy.android.app.utils.CameraUploadUtil
import mega.privacy.android.app.utils.JobUtil
import mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbishOrDeleted
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * GetCameraUploadAttributeListener
 *
 * @property context: Context
 */
class GetCameraUploadAttributeListener(val context: Context?) : MegaRequestListenerInterface {

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param error: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, error: MegaError) {
        if (request.type == MegaRequest.TYPE_GET_ATTR_USER
            || request.paramType == MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER
        ) {
            Timber.d("onRequestFinish errorCode: ${error.errorCode}")
            when (error.errorCode) {
                MegaError.API_OK -> {
                    val handles = getCUHandles(request)
                    Timber.d("Get Camera Upload Folders SUCCESS, Primary: ${handles.first}, Secondary: ${handles.second}")
                    // Guarantee Primary is called first, then Secondary Attributes
                    synchronized(this) {
                        handle(handles.first, false)
                        handle(handles.second, true)
                    }
                }
                MegaError.API_ENOENT -> {
                    // Happens when both Primary and Secondary folders are not set
                    Timber.w("First time setting Camera Upload Attributes")
                    CameraUploadUtil.initCUFolderFromScratch(context, false)

                    (context as? CameraUploadsService)?.onGetPrimaryFolderAttribute(
                        MegaApiJava.INVALID_HANDLE,
                        true
                    )
                }
                else -> {
                    Timber.e("Get Camera Upload Attributes Failed, Error Code: ${error.errorCode}, ${error.errorString}. Stopping Camera Uploads")
                    JobUtil.fireStopCameraUploadJob(context)
                }
            }
        }
    }

    /**
     * Get Primary and Secondary Folders handle from MegaRequest object
     *
     * @param request MegaRequest object which contains Primary and Secondary folders handle
     * @return A Pair with Primary and Secondary folder handle
     */
    private fun getCUHandles(request: MegaRequest): Pair<Long, Long> {
        var primaryHandle = MegaApiJava.INVALID_HANDLE
        var secondaryHandle = MegaApiJava.INVALID_HANDLE
        request.megaStringMap?.let {
            val h = it["h"]
            if (h != null) {
                primaryHandle = MegaApiJava.base64ToHandle(h)
            }
            val sh = it["sh"]
            if (sh != null) {
                secondaryHandle = MegaApiJava.base64ToHandle(sh)
            }
        } ?: run {
            Timber.e("MegaStringMap is NULL")
        }
        return Pair(primaryHandle, secondaryHandle)
    }

    /**
     * Process Primary or Secondary folder handle after getting them from the attributes
     *
     * @param handle      Folder handle
     * @param isSecondary Is the handle Secondary
     */
    private fun handle(handle: Long, isSecondary: Boolean) {
        Timber.d("Handle: $handle, isSecondary = $isSecondary")
        var shouldStopCameraUpload = false
        if (isNodeInRubbishOrDeleted(handle)) {
            Timber.w("Folder in rubbish bin, is secondary: %s", isSecondary)
            CameraUploadUtil.initCUFolderFromScratch(context, isSecondary)
        } else {
            shouldStopCameraUpload =
                CameraUploadUtil.compareAndUpdateLocalFolderAttribute(handle, isSecondary)
            // stop CameraUpload if destination has changed
            if (shouldStopCameraUpload) {
                Timber.e("Camera Uploads folder destination has changed. Stopping Camera Uploads")
                JobUtil.fireStopCameraUploadJob(context)
            }
            CameraUploadUtil.forceUpdateCameraUploadFolderIcon(isSecondary, handle)
        }
        if (!shouldStopCameraUpload) {
            Timber.d("Camera Uploads should not be stopped")
            if (!isSecondary) {
                // Primary is set first, camera upload will not get started, do not call a start service worker

                Timber.d("Call onGetPrimaryFolderAttribute with handle: $handle")
                (context as? CameraUploadsService)?.onGetPrimaryFolderAttribute(handle, false)
            } else {
                // Secondary is set after primary, camera upload will get started


                Timber.d("Call onGetSecondaryFolderAttribute with handle $handle")
                (context as? CameraUploadsService)?.onGetSecondaryFolderAttribute(handle)
            }
        }
    }
}
