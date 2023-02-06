package mega.privacy.android.domain.usecase

/**
 * Use case to set master key exported
 */
fun interface SetMasterKeyExported {

    /**
     * Invoke
     */
    suspend operator fun invoke()
}