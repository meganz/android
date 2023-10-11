package mega.privacy.android.data.wrapper

/**
 * Mega Application Wrapper
 */
interface ApplicationWrapper {

    /**
     * Sets the isIsHeartBeatAlive variable in MegaApplication
     * @param isAlive
     */
    fun setHeartBeatAlive(isAlive: Boolean)
}
