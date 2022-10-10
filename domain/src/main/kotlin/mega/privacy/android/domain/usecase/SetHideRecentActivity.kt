package mega.privacy.android.domain.usecase

/**
 * Set hide recent activity preference
 */
fun interface SetHideRecentActivity {
    /**
     * Invoke
     *
     * @param hide
     */
    suspend operator fun invoke(hide: Boolean)
}