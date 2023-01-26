package mega.privacy.android.domain.usecase

/**
 * Pass through use case to set Ask Always state
 */
fun interface SetStorageDownloadAskAlways {
    /**
     * Invoke
     * @param isChecked as [Boolean]
     */
    suspend operator fun invoke(isChecked: Boolean)
}