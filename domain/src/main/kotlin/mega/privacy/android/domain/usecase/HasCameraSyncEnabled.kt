package mega.privacy.android.domain.usecase

/**
 * Checks if camera uploads setting enabled exists.
 */
fun interface HasCameraSyncEnabled {

    /**
     * Invoke.
     */
    suspend operator fun invoke(): Boolean
}