package mega.privacy.android.domain.entity.camerauploads

/**
 * Interface that enumerates different actions to be performed by the Settings Camera Uploads
 */
sealed interface CameraUploadsSettingsAction {

    /**
     * An action to disable Media Uploads
     */
    data object DisableMediaUploads : CameraUploadsSettingsAction

    /**
     * An action to disable Camera Uploads
     */
    data object DisableCameraUploads : CameraUploadsSettingsAction
}
