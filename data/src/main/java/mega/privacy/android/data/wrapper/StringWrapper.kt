package mega.privacy.android.data.wrapper

/**
 * The interface for wrapping the static method regarding String
 */
interface StringWrapper {


    /**
     *Get localized progress size
     */
    fun getProgressSize(progress: Long, size: Long): String

    /**
     * Encode Base64 string
     *
     * @param string    String to be encoded
     * @return          Encoded Base64 string
     */
    fun encodeBase64(string: String): String

    /**
     * Decode Base64 string
     *
     * @param base64    Base64 string to be decoded
     * @return          Decoded base64 string
     */
    fun decodeBase64(base64: String): String
}
