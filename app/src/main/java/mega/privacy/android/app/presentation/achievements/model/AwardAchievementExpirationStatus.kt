package mega.privacy.android.app.presentation.achievements.model

/**
 * Represents the expiration status of an award achievement.
 */
sealed class AwardAchievementExpirationStatus {
    /**
     * Indicates that the award achievement is still valid and has a certain number of days left before expiration.
     *
     * @property daysLeft The number of days remaining before the award achievement expires.
     */
    data class Valid(val daysLeft: Long) : AwardAchievementExpirationStatus()

    /**
     * Indicates that the award achievement has expired.
     */
    object Expired : AwardAchievementExpirationStatus()

    /**
     * Indicates that the award achievement is permanent and does not expire.
     */
    object Permanent : AwardAchievementExpirationStatus()
}