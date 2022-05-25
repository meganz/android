package mega.privacy.android.app.fcm

/**
 * Generic push message object, used to unify corresponding platform dependent RemoteMessage object.
 *
 * @property from
 * @property originalPriority   Just for log and debug purpose.
 * @property priority           Just for log and debug purpose.
 * @property data               Stores couples of info sent from server, but the client only use: silent, type, email.
 * @property pushMessage        [PushMessage] containing the required info to use for pushes.
 */
data class MegaRemoteMessage(
    private val from: String?,
    private val originalPriority: Int,
    private val priority: Int,
    private val data: Map<String, String>?,
    val pushMessage: PushMessage = PushMessage(data)
) {

    override fun toString(): String =
        "Handle message from: $from , " +
                "which type is: ${pushMessage.type}. " +
                "Original priority is ${originalPriority}, " +
                "priority is $priority"
}