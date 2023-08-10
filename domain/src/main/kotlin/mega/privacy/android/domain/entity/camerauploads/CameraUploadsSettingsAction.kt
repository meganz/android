package mega.privacy.android.domain.entity.camerauploads

/**
 * Camera Uploads Settings Action
 */
sealed interface CameraUploadsSettingsAction {

    /**
     * DisableMediaUpload
     */
    object DisableMediaUploads : CameraUploadsSettingsAction

    /**
     * RefreshSettings
     */
    object RefreshSettings : CameraUploadsSettingsAction
}
