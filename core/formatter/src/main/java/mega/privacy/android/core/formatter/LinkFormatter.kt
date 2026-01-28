package mega.privacy.android.core.formatter

/**
 * Utility object for extracting components from MEGA links.
 * Supports both old format (#!) and new format (#) link structures.
 */
object LinkFormatter {
    /**
     * Extracts the link portion from a MEGA link that includes a decryption key.
     * Supports both old format (#!) and new format (#) link structures.
     *
     * @param linkWithKey The complete MEGA link including the decryption key.
     * @return The link without the decryption key, or null if the link format is invalid.
     */
    fun extractLinkWithoutKey(linkWithKey: String): String? =
        extractLinkComponent(linkWithKey, extractKey = false)

    /**
     * Extracts the decryption key from a MEGA link.
     * Supports both old format (#!) and new format (#) link structures.
     *
     * @param linkWithKey The complete MEGA link including the decryption key.
     * @return The decryption key portion of the link, or null if the link format is invalid.
     */
    fun extractDecryptionKey(linkWithKey: String): String? =
        extractLinkComponent(linkWithKey, extractKey = true)

    /**
     * Extracts either the link or decryption key component from a MEGA link.
     * Supports both old format (#!) and new format (#) link structures.
     *
     * @param linkWithKey The complete MEGA link including the decryption key.
     * @param extractKey If true, returns the decryption key. If false, returns the link without the key.
     * @return The extracted link component or decryption key, or null if the link format is invalid.
     */
    private fun extractLinkComponent(linkWithKey: String, extractKey: Boolean): String? {
        return if (linkWithKey.contains("#!") || linkWithKey.contains("#F!")) {
            // old file or folder link format
            val parts = linkWithKey.split("!")
            if (parts.size == 3) {
                if (extractKey) parts[2] else "${parts[0]}!${parts[1]}"
            } else null
        } else {
            // new file or folder link format
            val parts = linkWithKey.split("#")
            if (parts.size == 2) {
                if (extractKey) parts[1] else parts[0]
            } else null
        }
    }
}
