package mega.privacy.android.domain.usecase

/**
 * Use case for getting the camera upload folder name
 */
fun interface GetCameraUploadFolderName {

    /**
     * Invoke
     *
     * @param isSecondary if folder name is camera or media uploads
     */
    suspend operator fun invoke(isSecondary: Boolean): String
}
