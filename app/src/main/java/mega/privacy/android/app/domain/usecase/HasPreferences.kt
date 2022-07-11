package mega.privacy.android.app.domain.usecase

/**
 * Use case to check if preferences exist
 */
fun interface HasPreferences {

    /**
     * Invoke
     *
     * @return do preferences exist
     */
    operator fun invoke(): Boolean
}
