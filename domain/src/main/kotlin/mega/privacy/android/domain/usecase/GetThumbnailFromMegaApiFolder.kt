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
     * @param finishedCallback callback of getting thumbnail finished
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        path: String,
        finishedCallback: (nodeHandle: Long) -> Unit,
    )
}