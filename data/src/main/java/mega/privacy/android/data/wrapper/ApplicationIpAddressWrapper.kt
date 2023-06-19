package mega.privacy.android.data.wrapper

/**
 * Get and set ip address of Application property
 */
interface ApplicationIpAddressWrapper {

    /**
     * set ip address
     * @param ipAddress [String]
     */
    fun setIpAddress(ipAddress: String?)

    /**
     * get ip address
     * @return ip address [String]
     */
    fun getIpAddress(): String?
}
