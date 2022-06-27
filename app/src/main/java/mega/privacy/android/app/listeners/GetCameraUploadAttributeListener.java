package mega.privacy.android.app.listeners;

import static mega.privacy.android.app.utils.CameraUploadUtil.compareAndUpdateLocalFolderAttribute;
import static mega.privacy.android.app.utils.CameraUploadUtil.forceUpdateCameraUploadFolderIcon;
import static mega.privacy.android.app.utils.CameraUploadUtil.initCUFolderFromScratch;
import static mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbishOrDeleted;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER;
import static nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER;

import android.content.Context;

import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.utils.JobUtil;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaStringMap;
import timber.log.Timber;

public class GetCameraUploadAttributeListener extends BaseListener {

    public GetCameraUploadAttributeListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != TYPE_GET_ATTR_USER || request.getParamType() != USER_ATTR_CAMERA_UPLOADS_FOLDER) {
            return;
        }

        if (e.getErrorCode() == MegaError.API_OK) {
            long[] handles = getCUHandles(request);
            Timber.d("Get CU folders successfully primary: %d, secondary: %d", handles[0], handles[1]);

            synchronized (this) {
                handle(handles[0], false, e);
                handle(handles[1], true, e);
            }
        } else if (e.getErrorCode() == MegaError.API_ENOENT) {
            // only when both CU and MU are not set, will return API_ENOENT
            Timber.d("First time set CU attribute.");
            initCUFolderFromScratch(context, false);

            if (context instanceof CameraUploadsService) {
                ((CameraUploadsService) context).onGetPrimaryFolderAttribute(INVALID_HANDLE, e.getErrorCode(), true);
            }
        } else {
            Timber.w("Get CU attributes failed, error code: %d, %s", e.getErrorCode(), e.getErrorString());
            JobUtil.fireStopCameraUploadJob(context);
        }
    }

    /**
     * Get CU and MU folders handle from MegaRequest object.
     *
     * @param request MegaRequest object which contains CU and MU folders handle.
     * @return An array with CU folder handle at the first element, and MU folder handle at the second element.
     */
    private long[] getCUHandles(MegaRequest request) {
        long primaryHandle = INVALID_HANDLE, secondaryHandle = INVALID_HANDLE;
        MegaStringMap map = request.getMegaStringMap();

        if (map != null) {
            String h = map.get("h");
            if (h != null) {
                primaryHandle = MegaApiJava.base64ToHandle(h);
            }

            String sh = map.get("sh");
            if (sh != null) {
                secondaryHandle = MegaApiJava.base64ToHandle(sh);
            }
        } else {
            Timber.e("MegaStringMap is null.");
        }

        return new long[]{primaryHandle, secondaryHandle};
    }

    /**
     * Process CU or MU folder handle after get them from CU attributes.
     *
     * @param handle      Folder handle.
     * @param isSecondary Is the handle CU handle or MU handle.
     * @param e           MegaError object.
     */
    private void handle(long handle, boolean isSecondary, MegaError e) {
        boolean shouldStopCameraUpload = false;

        if (isNodeInRubbishOrDeleted(handle)) {
            Timber.d("Folder in rubbish bin, is secondary: %s", isSecondary);
            initCUFolderFromScratch(context, isSecondary);
        } else {
            shouldStopCameraUpload = compareAndUpdateLocalFolderAttribute(handle, isSecondary);
            // stop CameraUpload if destination has changed
            if (shouldStopCameraUpload) {
                JobUtil.fireStopCameraUploadJob(context);
            }

            forceUpdateCameraUploadFolderIcon(isSecondary, handle);
        }

        if (!shouldStopCameraUpload && context instanceof CameraUploadsService) {
            if (isSecondary) {
                ((CameraUploadsService) context).onGetSecondaryFolderAttribute(handle, e.getErrorCode());
            } else {
                ((CameraUploadsService) context).onGetPrimaryFolderAttribute(handle, e.getErrorCode(), false);
            }
        }
    }
}