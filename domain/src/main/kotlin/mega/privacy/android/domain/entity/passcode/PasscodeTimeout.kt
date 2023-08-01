package mega.privacy.android.domain.entity.passcode

/**
 * Passcode timeout
 */
sealed interface PasscodeTimeout {
    /**
     *  Immediate
     */
    object Immediate : PasscodeTimeout

    /**
     * TimeSpan
     *
     * @property milliseconds
     */
    data class TimeSpan(val milliseconds: Long) : PasscodeTimeout {
        companion object {
            /**
             * Of seconds
             *
             * @param seconds
             */
            fun ofSeconds(seconds: Int) = TimeSpan(seconds * 1000L)

            /**
             * Of minutes
             *
             * @param minutes
             */
            fun ofMinutes(minutes: Int) = TimeSpan.ofSeconds(minutes * 60)
        }
    }
}
