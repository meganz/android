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
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_GET_ATTR_USER ||
            request.paramType == MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER
        ) {
            when (e.errorCode) {
                MegaError.API_OK -> {
                    val handles = getCUHandles(request)
                    Timber.d("Get CU folders successfully primary: %d, secondary: %d",
                        handles[0],
                        handles[1])
                    synchronized(this) {
                        handle(handles[0], false, e)
                        handle(handles[1], true, e)
                    }
                }

                MegaError.API_ENOENT -> {
                    // only when both CU and MU are not set, will return API_ENOENT
                    Timber.d("First time set CU attribute.")
                    CameraUploadUtil.initCUFolderFromScratch(context, false)
                    (context as? CameraUploadsService)?.onGetPrimaryFolderAttribute(MegaApiJava.INVALID_HANDLE,
                        e.errorCode,
                        true)
                }

                else -> {
                    Timber.w("Get CU attributes failed, error code: %d, %s",
                        e.errorCode,
                        e.errorString)
                    JobUtil.fireStopCameraUploadJob(context)
                }
            }
        }
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
     * Get CU and MU folders handle from MegaRequest object.
     *
     * @param request MegaRequest object which contains CU and MU folders handle.
     * @return An array with CU folder handle at the first element, and MU folder handle at the second element.
     */
    private fun getCUHandles(request: MegaRequest): LongArray {
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
            Timber.e("MegaStringMap is null.")
        }
        return longArrayOf(primaryHandle, secondaryHandle)
    }

    /**
     * Process CU or MU folder handle after get them from CU attributes.
     *
     * @param handle      Folder handle.
     * @param isSecondary Is the handle CU handle or MU handle.
     * @param e           MegaError object.
     */
    private fun handle(handle: Long, isSecondary: Boolean, e: MegaError) {
        var shouldStopCameraUpload = false
        if (isNodeInRubbishOrDeleted(handle)) {
            Timber.d("Folder in rubbish bin, is secondary: %s", isSecondary)
            CameraUploadUtil.initCUFolderFromScratch(context, isSecondary)
        } else {
            shouldStopCameraUpload =
                CameraUploadUtil.compareAndUpdateLocalFolderAttribute(handle, isSecondary)
            // stop CameraUpload if destination has changed
            if (shouldStopCameraUpload) {
                JobUtil.fireStopCameraUploadJob(context)
            }
            CameraUploadUtil.forceUpdateCameraUploadFolderIcon(isSecondary, handle)
        }
        if (!shouldStopCameraUpload) {
            if (isSecondary) {
                (context as? CameraUploadsService)?.onGetSecondaryFolderAttribute(handle,
                    e.errorCode)
            } else {
                (context as? CameraUploadsService)?.onGetPrimaryFolderAttribute(handle,
                    e.errorCode,
                    false)
            }
        }
    }
}