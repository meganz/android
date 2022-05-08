package mega.privacy.android.app.domain.usecase

/**
 * The use case interface to get camera upload folder handler
 */
interface GetCameraUploadFolder {

    /**
     * camera upload folder handler string
     */
    operator fun invoke(): String?
}