package mega.privacy.android.app.constants

/**
 * The constants regarding Broadcast
 */
object BroadcastConstants {
    //    Broadcasts' IntentFilter
    const val BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE = "INTENT_FILTER_CONTACT_UPDATE"

    //    Broadcasts' actions
    const val ACTION_UPDATE_NICKNAME = "ACTION_UPDATE_NICKNAME"
    const val ACTION_UPDATE_FIRST_NAME = "ACTION_UPDATE_FIRST_NAME"
    const val ACTION_UPDATE_LAST_NAME = "ACTION_UPDATE_LAST_NAME"
    const val ACTION_UPDATE_CREDENTIALS = "ACTION_UPDATE_CREDENTIALS"

    //    Broadcasts' extras
    const val TYPE_SHARE = "TYPE_SHARE"
    const val NUMBER_FILES = "NUMBER_FILES"
    const val EXTRA_USER_HANDLE = "USER_HANDLE"
    const val SNACKBAR_TEXT = "SNACKBAR_TEXT"
    const val ACTION_TYPE = "ACTION_TYPE"
    const val INVALID_ACTION = -1
    const val ERROR_MESSAGE_TEXT = "ERROR_MESSAGE_TEXT"
}
