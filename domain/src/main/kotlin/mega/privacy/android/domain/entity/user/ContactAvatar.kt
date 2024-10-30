package mega.privacy.android.domain.entity.user

/**
 * Contact avatar
 *
 */
sealed interface ContactAvatar {
    val id: UserId

    /**
     * With id
     *
     * @property id
     */
    data class WithId(override val id: UserId) : ContactAvatar

    /**
     * With email
     *
     * @property email
     * @property id
     */
    data class WithEmail(val email: String, override val id: UserId) : ContactAvatar

    companion object {
        /**
         * Utility constructor
         */
        operator fun invoke(id: UserId): ContactAvatar = WithId(id)

        /**
         * Utility constructor
         */
        operator fun invoke(email: String, id: UserId): ContactAvatar = WithEmail(email, id)
    }
}