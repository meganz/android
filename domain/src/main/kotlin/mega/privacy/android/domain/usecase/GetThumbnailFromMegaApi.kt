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
     * @param finishedCallback callback of getting thumbnail finished
     */
    suspend operator fun invoke(
        nodeHandle: Long,
        path: String,
        finishedCallback: (nodeHandle: Long) -> Unit,
    )
}