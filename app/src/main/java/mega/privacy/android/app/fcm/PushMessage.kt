package mega.privacy.android.app.fcm

import androidx.work.Data

/**
 *
 * @property remoteData Map containing all the info of the push message.
 * @property type       Push type or null if the map is empty.
 * @property email      Email or null if the map is empty.
 * @property silent     Value indicating if should beep or not, null if the map is empty.
 */
data class PushMessage(
    private val remoteData: Map<String, String>? = null,
    val type: String? = remoteData?.get(KEY_TYPE),
    val email: String? = remoteData?.get(KEY_EMAIL),
    val silent: String? = remoteData?.get(KEY_SILENT)
) {

    /**
     * Checks if the push should beep.
     *
     * @return True if the push should beep, false otherwise.
     */
    fun shouldBeep(): Boolean = NO_BEEP != silent

    /**
     * Builds a [Data] containing all the relevant info of the [PushMessage].
     *
     * @return [Data] object.
     */
    fun toData(): Data =
        Data.Builder()
            .putString(KEY_TYPE, type)
            .putString(KEY_EMAIL, email)
            .putString(KEY_SILENT, silent)
            .build()

    companion object {
        private const val KEY_TYPE = "type"
        private const val KEY_EMAIL = "email"
        private const val KEY_SILENT = "silent"
        private const val NO_BEEP = "1"

        /**
         * Gets a [PushMessage] from [Data].
         *
         * @return [PushMessage] object.
         */
        fun Data.toPushMessage(): PushMessage =
            PushMessage(
                type = getString(KEY_TYPE),
                email = getString(KEY_EMAIL),
                silent = getString(KEY_SILENT)
            )
    }
}
