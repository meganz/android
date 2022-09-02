package mega.privacy.android.domain.usecase

/**
 * Is Camera Upload preference enabled
 */
fun interface IsCameraSyncPreferenceEnabled {
    /**
     * Invoke the use case
     *
     * @return the current value of the preference
     */
    operator fun invoke(): Boolean
}
