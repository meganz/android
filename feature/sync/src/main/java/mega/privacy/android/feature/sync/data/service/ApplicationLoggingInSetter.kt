package mega.privacy.android.feature.sync.data.service

/**
 * Sets LoggingIn variable in MegaApplication
 *
 * This is used to avoid multiple logins at the same time.
 * If two logins start a the same time, the account session will be broken forever.
 * The solution is temporary and will be removed when the login process is refactored.
 */
interface ApplicationLoggingInSetter {

    /**
     * Sets the isLoggingIn variable in MegaApplication
     */
    fun setLoggingIn(loggingIn: Boolean)

    /**
     * Gets the isLoggingIn variable in MegaApplication
     */
    fun isLoggingIn(): Boolean
}