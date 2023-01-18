package mega.privacy.android.domain.usecase

/**
 * The use case for getting thumbnail from MegaApiFolder
 */
fun interface GetThumbnailFromMegaApiFolder {

    /**
     * Get thumbnail from MegaApiFolder
     *
     * @param nodeHandle node handle
     * @param path thumbnail path
     */
    suspend operator fun invoke(nodeHandle: Long, path: String): Long?
}