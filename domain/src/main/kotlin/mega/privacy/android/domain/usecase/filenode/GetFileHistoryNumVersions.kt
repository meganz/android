package mega.privacy.android.domain.usecase.filenode

/**
 * Gets the number of versions the file has
 */
fun interface GetFileHistoryNumVersions {
    /**
     * Gets the number of versions the file represented by nodeHandle has
     * @param nodeHandle the handle of the file from which we want to get the data
     * @return the number of history versions or 0 if the file is not found or has no versions
     */
    suspend operator fun invoke(nodeHandle: Long): Int
}