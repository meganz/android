package mega.privacy.android.domain.entity.pushes

/**
 *
 * @property remoteData Map containing all the info of the push message.
 * @property type       Push type or null if the map is empty.
 * @property email      Email or null if the map is empty.
 * @property silent     Value indicating if should beep or not, null if the map is empty.
 * @property chatId     Base64-encoded chat identifier.
 */
data class PushMessage(
    private val remoteData: Map<String, String>? = null,
    val type: String? = remoteData?.get(KEY_TYPE),
    val email: String? = remoteData?.get(KEY_EMAIL),
    val silent: String? = remoteData?.get(KEY_SILENT),
    val chatId: String? = remoteData?.get(KEY_CHAT_ID),
) {

    /**
     * Checks if the push should beep.
     *
     * @return True if the push should beep, false otherwise.
     */
    fun shouldBeep(): Boolean = NO_BEEP != silent

    companion object {
        /**
         * Key defining push message type.
         */
        const val KEY_TYPE = "type"

        /**
         * Key defining email.
         */
        const val KEY_EMAIL = "email"

        /**
         * Key defining chat id.
         */
        const val KEY_CHAT_ID = "chatid"

        /**
         * Key defining silent.
         */
        const val KEY_SILENT = "silent"
        private const val NO_BEEP = "1"
    }
}
