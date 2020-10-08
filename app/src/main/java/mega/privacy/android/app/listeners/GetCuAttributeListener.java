package mega.privacy.android.app.listeners;

import android.content.Context;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.jobservices.CameraUploadsService;
import mega.privacy.android.app.utils.JobUtil;
import mega.privacy.android.app.utils.MegaNodeUtil;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaStringMap;

import static mega.privacy.android.app.utils.CameraUploadUtil.compareAndUpdateLocalFolderAttribute;
import static mega.privacy.android.app.utils.CameraUploadUtil.forceUpdateCameraUploadFolderIcon;
import static mega.privacy.android.app.utils.CameraUploadUtil.initCUFolderFromScratch;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.MegaNodeUtil.isNodeInRubbishOrDeleted;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaApiJava.USER_ATTR_CAMERA_UPLOADS_FOLDER;
import static nz.mega.sdk.MegaRequest.TYPE_GET_ATTR_USER;

public class GetCuAttributeListener extends BaseListener {

    public GetCuAttributeListener(Context context) {
        super(context);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() != TYPE_GET_ATTR_USER || request.getParamType() != USER_ATTR_CAMERA_UPLOADS_FOLDER) {
            return;
        }
        if (e.getErrorCode() == MegaError.API_OK) {
            long[] handles = getCUHandles(request);
            logDebug("Get CU folders successfully primary: " + handles[0] + "(" + MegaNodeUtil.getNodeName(handles[0]) + ") secondary: " + handles[1] + "(" + MegaNodeUtil.getNodeName(handles[1]) + ")");
            synchronized (this) {
                handle(api, handles[0], false, e);
                handle(api, handles[1], true, e);
            }
        } else if (e.getErrorCode() == MegaError.API_ENOENT) {
            // only when both CU and MU are not set, will return API_ENOENT
            logDebug("First time set CU attribute.");
            initCUFolderFromScratch(context, false);
            if (context instanceof CameraUploadsService) {
                // The unique process run within shoudRun method in CameraUploadsService
                ((CameraUploadsService) context).onGetPrimaryFolderAttribute(INVALID_HANDLE, e.getErrorCode(), true);
            }
        } else {
            logWarning("Get CU attributes failed, error code: " + e.getErrorCode() + ", " + e.getErrorString());
            JobUtil.stopRunningCameraUploadService(context);
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
            logError("MegaStringMap is null.");
        }
        return new long[]{primaryHandle, secondaryHandle};
    }

    /**
     * Process CU or MU folder handle after get them from CU attributes.
     *
     * @param handle Folder handle.
     * @param isSecondary Is the handle CU handle or MU handle.
     * @param e MegaError object.
     */
    private void handle(MegaApiJava api, long handle, boolean isSecondary, MegaError e) {
        boolean shouldCUStop = false;
        if (isNodeInRubbishOrDeleted(handle)) {
            logDebug("Folder in rubbish bin, is secondary: " + isSecondary);
            initCUFolderFromScratch(context, isSecondary);
        } else {
            shouldCUStop = compareAndUpdateLocalFolderAttribute(handle, isSecondary);
            //stop CU if destination has changed
            if (shouldCUStop && CameraUploadsService.isServiceRunning) {
                JobUtil.stopRunningCameraUploadService(context);
            }

            //notify manager activity to update UI
            if (!(context instanceof MegaApplication)) {
                forceUpdateCameraUploadFolderIcon(isSecondary, handle);
            }
        }
        if (!shouldCUStop && context instanceof CameraUploadsService) {
            // The unique process run within shoudRun method in CameraUploadsService
            if (isSecondary) {
                ((CameraUploadsService) context).onGetSecondaryFolderAttribute(handle, e.getErrorCode());
            } else {
                ((CameraUploadsService) context).onGetPrimaryFolderAttribute(handle, e.getErrorCode(), false);
            }
        }
    }
}