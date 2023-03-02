package mega.privacy.android.domain.usecase

/**
 * The use case for get local link by http server
 */
fun interface GetFileUrlByNodeHandle {

    /**
     * Get file url by node handle
     *
     * @param handle node handle
     * @return local link
     */
    suspend operator fun invoke(handle: Long): String?
}