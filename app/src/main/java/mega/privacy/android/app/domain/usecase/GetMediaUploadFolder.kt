package mega.privacy.android.app.domain.usecase

/**
 * The use case interface to get media upload folder handler
 */
interface GetMediaUploadFolder {

    /**
     * media upload folder handler string
     */
    operator fun invoke(): String?
}