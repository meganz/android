package mega.privacy.android.domain.usecase

/**
 * Interface to set initial CU preferences
 */
interface SetInitialCUPreferences {
    /**
     * Set the initial Camera Uploads preferences
     */
    suspend operator fun invoke()
}
