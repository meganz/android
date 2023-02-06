package mega.privacy.android.domain.usecase

/**
 * Use case to get export master key
 */
fun interface GetExportMasterKey {
    /**
     * Invoke
     * @return master key as [String]
     */
    suspend operator fun invoke(): String?
}