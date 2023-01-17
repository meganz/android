package mega.privacy.android.domain.usecase

/**
 * The use case for getting thumbnail from MegaApi
 */
fun interface GetThumbnailFromMegaApi {

    /**
     * Get thumbnail from MegaApi
     *
     * @param nodeHandle node handle
     * @param path thumbnail path
     */
    suspend operator fun invoke(nodeHandle: Long, path: String): Long?
}