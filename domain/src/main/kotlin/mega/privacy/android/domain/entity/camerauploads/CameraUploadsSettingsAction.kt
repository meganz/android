package mega.privacy.android.domain.entity.camerauploads

/**
 * Interface that enumerates different actions to be performed by the Settings Camera Uploads
 */
sealed interface CameraUploadsSettingsAction {

    /**
     * Enable Media Uploads action has been performed
     */
    data object MediaUploadsEnabled : CameraUploadsSettingsAction

    /**
     * An action to disable Media Uploads
     */
    data object DisableMediaUploads : CameraUploadsSettingsAction

    /**
     * Disable Media Uploads action has been performed
     */
    data object MediaUploadsDisabled : CameraUploadsSettingsAction

    /**
     * Media Uploads MEGA folder node has been changed
     */
    data object MediaUploadsMegaNodeChanged : CameraUploadsSettingsAction

    /**
     * Media Uploads local folder has been changed
     */
    data object MediaUploadsLocalFolderChanged : CameraUploadsSettingsAction

    /**
     * Enable Camera Uploads action has been performed
     */
    data object CameraUploadsEnabled : CameraUploadsSettingsAction

    /**
     * An action to disable Camera Uploads
     */
    data object DisableCameraUploads : CameraUploadsSettingsAction

    /**
     * Disable Camera Uploads action has been performed
     */
    data object CameraUploadsDisabled : CameraUploadsSettingsAction

    /**
     * Camera Uploads MEGA folder node has been changed
     */
    data object CameraUploadsMegaNodeChanged : CameraUploadsSettingsAction

    /**
     * Camera Uploads local folder has been changed
     */
    data object CameraUploadsLocalFolderChanged : CameraUploadsSettingsAction
}
