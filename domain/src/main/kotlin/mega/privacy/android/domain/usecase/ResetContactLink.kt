package mega.privacy.android.domain.usecase

/**
 * Use case to reset contact link
 */
fun interface ResetContactLink {

    /**
     * invoke method
     *
     * @return new contact link
     */
    suspend operator fun invoke(): String
}