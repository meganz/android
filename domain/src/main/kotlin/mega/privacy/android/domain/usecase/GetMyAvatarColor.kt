package mega.privacy.android.domain.usecase

/**
 * Get avatar color
 */
fun interface GetMyAvatarColor {
    /**
     * get color from the avatar
     *
     * @return the color of avatar
     */
    suspend operator fun invoke(): Int
}