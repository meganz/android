package mega.privacy.android.domain.usecase

/**
 * Check whether Achievements are enabled or not
 */
fun interface AreAccountAchievementsEnabled {

    /**
     * Invoke.
     *
     * @return [Boolean]
     */
    suspend operator fun invoke(): Boolean
}
