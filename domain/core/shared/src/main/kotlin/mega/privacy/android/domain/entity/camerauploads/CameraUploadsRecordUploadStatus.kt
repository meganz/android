package mega.privacy.android.domain.entity.camerauploads

/**
 * Camera Uploads record upload status
 *
 * Identify the status of the upload transfer for each record through CU
 */
enum class CameraUploadsRecordUploadStatus {
    /**
     * The transfer has not started and the file is waiting to be uploaded
     */
    PENDING,

    /**
     * The transfer has started but has not completed yet
     */
    STARTED,

    /**
     * The file has been successfully uploaded through CU
     */
    UPLOADED,

    /**
     * The transfer failed either during the processing of the file before uplaod or during the transfer
     */
    FAILED,

    /**
     * The file already exists in the cloud target folder
     */
    ALREADY_EXISTS,

    /**
     * The file does not exist in the local folder
     */
    LOCAL_FILE_NOT_EXIST,

    /**
     * The file has been copied from another cloud folder
     */
    COPIED,
}
