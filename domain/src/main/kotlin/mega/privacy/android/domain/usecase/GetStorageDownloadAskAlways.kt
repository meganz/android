package mega.privacy.android.domain.usecase

/**
 * Pass through use case to get Ask Always state
 */
fun interface GetStorageDownloadAskAlways {
    /**
     * Invoke
     * @return storageAskAlways as [Boolean]
     */
    suspend operator fun invoke(): Boolean
}