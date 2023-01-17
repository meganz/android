package mega.privacy.android.domain.usecase

/**
 * The use case for MegaApi sets the maximum buffer size for the internal buffer
 */
fun interface MegaApiHttpServerSetMaxBufferSize {

    /**
     * MegaApi sets the maximum buffer size for the internal buffer
     *
     * @param bufferSize Maximum buffer size (in bytes) or a number <= 0 to use the
     *                   internal default value
     */
    suspend operator fun invoke(bufferSize: Int)
}