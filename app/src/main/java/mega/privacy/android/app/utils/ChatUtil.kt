package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.MarqueeTextView
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.megachat.GroupChatInfoActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.utils.ContactUtil.isContact
import mega.privacy.android.app.utils.TextUtil.isTextEmpty
import mega.privacy.android.app.utils.TextUtil.removeFormatPlaceholder
import mega.privacy.android.app.utils.TimeUtils.getCorrectStringDependingOnOptionSelected
import mega.privacy.android.app.utils.TimeUtils.isUntilThisMorning
import mega.privacy.android.domain.entity.settings.ChatSettings
import mega.privacy.android.navigation.MegaNavigatorEntryPoint
import mega.privacy.android.navigation.OpenTextEditorParams
import mega.privacy.android.shared.original.core.ui.controls.controlssliders.MegaSwitch
import mega.privacy.android.thirdpartylib.twemoji.EmojiManager
import mega.privacy.android.thirdpartylib.twemoji.EmojiUtilsShortcodes
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser.VISIBILITY_VISIBLE
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

object ChatUtil {

    const val AUDIOFOCUS_DEFAULT: Int = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
    const val STREAM_MUSIC_DEFAULT: Int = AudioManager.STREAM_MUSIC

    /**
     * Where is the status icon placed, according to the design,
     * according to the design,
     * on dark mode the status icon image is different based on the place where it's placed.
     */
    enum class StatusIconLocation {

        /**
         * On chat list
         * Contact list
         * Contact info
         * Flat app bar no chat room
         */
        STANDARD,

        /**
         * Raised app bar on chat room
         */
        APPBAR,

        /**
         * On nav drawer
         * Bottom sheets
         */
        DRAWER
    }

    @JvmStatic
    fun isVoiceClip(name: String): Boolean =
        MimeTypeList.typeForName(name).isAudioVoiceClip

    @JvmStatic
    fun getVoiceClipDuration(node: MegaNode): Long =
        if (node.duration <= 0) 0 else node.duration * 1000L

    /* Get the height of the action bar */
    @JvmStatic
    fun getActionBarHeight(activity: Activity?, resources: Resources?): Int {
        var actionBarHeight = 0
        val tv = TypedValue()
        if (activity != null && activity.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, resources!!.displayMetrics)
        }
        return actionBarHeight
    }

    private fun getRealLength(text: CharSequence): Int {
        val length = text.length

        val emojisFound = EmojiManager.getInstance().findAllEmojis(text)
        var count = 0
        if (emojisFound.size > 0) {
            for (i in emojisFound.indices) {
                count += emojisFound[i].end - emojisFound[i].start
            }
            return length + count
        }
        return length
    }

    @JvmStatic
    fun getMaxAllowed(text: CharSequence?): Int {
        val realLength = getRealLength(text ?: "")
        if (realLength > Constants.MAX_ALLOWED_CHARACTERS_AND_EMOJIS) {
            return text?.length ?: 0
        }
        return Constants.MAX_ALLOWED_CHARACTERS_AND_EMOJIS
    }

    @JvmStatic
    fun isAllowedTitle(text: String): Boolean =
        getMaxAllowed(text) != text.length || getRealLength(text) == Constants.MAX_ALLOWED_CHARACTERS_AND_EMOJIS

    @JvmStatic
    fun showConfirmationRemoveChatLink(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.action_delete_link)
            .setMessage(R.string.context_remove_chat_link_warning_text)
            .setPositiveButton(R.string.delete_button) { _, _ ->
                if (context is GroupChatInfoActivity) {
                    context.removeChatLink()
                }
            }
            .setNegativeButton(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button, null)
            .show()
    }

    @JvmStatic
    fun getMegaChatMessage(
        context: Context,
        megaChatApi: MegaChatApiAndroid,
        chatId: Long,
        messageId: Long,
    ): MegaChatMessage? =
        if (context is NodeAttachmentHistoryActivity) {
            megaChatApi.getMessageFromNodeHistory(chatId, messageId)
        } else {
            megaChatApi.getMessage(chatId, messageId)
        }

    @JvmStatic
    fun converterShortCodes(text: String?): String? {
        if (text.isNullOrEmpty()) return text
        return EmojiUtilsShortcodes.emojify(text)
    }

    /**
     * Sets the contact status icon
     *
     * @param userStatus       contact's status
     * @param contactStateIcon view in which the status icon has to be set
     * @param where            Where the icon is placed.
     */
    @JvmStatic
    fun setContactStatus(userStatus: Int, contactStateIcon: ImageView?, where: StatusIconLocation) {
        if (contactStateIcon == null) {
            return
        }

        val context = contactStateIcon.context
        contactStateIcon.visibility = View.VISIBLE

        val statusImageResId = getIconResourceIdByLocation(context, userStatus, where)

        // Hide the icon ImageView.
        if (statusImageResId == 0) {
            contactStateIcon.visibility = View.GONE
        } else {
            contactStateIcon.setImageResource(statusImageResId)
        }
    }

    /**
     * Sets the contact status icon and status text
     *
     * @param userStatus          contact's status
     * @param textViewContactIcon view in which the status icon has to be set
     * @param contactStateText    view in which the status text has to be set
     * @param where               The status icon image resource is different based on the place where it's placed.
     */
    @JvmStatic
    fun setContactStatusParticipantList(
        userStatus: Int,
        textViewContactIcon: ImageView,
        contactStateText: TextView?,
        where: StatusIconLocation,
    ) {
        val app = MegaApplication.getInstance()
        val context = app.applicationContext
        val statusImageResId = getIconResourceIdByLocation(context, userStatus, where)

        if (statusImageResId == 0) {
            textViewContactIcon.visibility = View.GONE
        } else {
            val drawable = ContextCompat.getDrawable(MegaApplication.getInstance().applicationContext, statusImageResId)
            textViewContactIcon.setImageDrawable(drawable)
            textViewContactIcon.visibility = View.VISIBLE
        }

        if (contactStateText == null) {
            return
        }

        contactStateText.visibility = View.VISIBLE

        when (userStatus) {
            MegaChatApi.STATUS_ONLINE ->
                contactStateText.text = context.getString(R.string.online_status)

            MegaChatApi.STATUS_AWAY ->
                contactStateText.text = context.getString(R.string.away_status)

            MegaChatApi.STATUS_BUSY ->
                contactStateText.text = context.getString(R.string.busy_status)

            MegaChatApi.STATUS_OFFLINE ->
                contactStateText.text = context.getString(R.string.offline_status)

            MegaChatApi.STATUS_INVALID -> contactStateText.visibility = View.GONE
            else -> contactStateText.visibility = View.GONE
        }
    }

    /**
     * Get status icon image resource id by display mode and where the icon is placed.
     *
     * @param context    Context object.
     * @param userStatus User online status.
     * @param where      Where the icon is placed.
     * @return Image resource id based on where the icon is placed.
     * NOTE: when the user has an invalid online status, returns 0.
     * Caller should verify the return value, 0 is an invalid value for resource id.
     */
    @JvmStatic
    fun getIconResourceIdByLocation(context: Context, userStatus: Int, where: StatusIconLocation): Int {
        var statusImageResId = 0

        when (userStatus) {
            MegaChatApi.STATUS_ONLINE -> {
                statusImageResId = if (Util.isDarkMode(context)) {
                    when (where) {
                        StatusIconLocation.STANDARD -> R.drawable.ic_online_dark_standard
                        StatusIconLocation.DRAWER -> R.drawable.ic_online_dark_drawer
                        StatusIconLocation.APPBAR -> R.drawable.ic_online_dark_appbar
                    }
                } else {
                    R.drawable.ic_online_light
                }
            }

            MegaChatApi.STATUS_AWAY -> {
                statusImageResId = if (Util.isDarkMode(context)) {
                    when (where) {
                        StatusIconLocation.STANDARD -> R.drawable.ic_away_dark_standard
                        StatusIconLocation.DRAWER -> R.drawable.ic_away_dark_drawer
                        StatusIconLocation.APPBAR -> R.drawable.ic_away_dark_appbar
                    }
                } else {
                    R.drawable.ic_away_light
                }
            }

            MegaChatApi.STATUS_BUSY -> {
                statusImageResId = if (Util.isDarkMode(context)) {
                    when (where) {
                        StatusIconLocation.STANDARD -> R.drawable.ic_busy_dark_standard
                        StatusIconLocation.DRAWER -> R.drawable.ic_busy_dark_drawer
                        StatusIconLocation.APPBAR -> R.drawable.ic_busy_dark_appbar
                    }
                } else {
                    R.drawable.ic_busy_light
                }
            }

            MegaChatApi.STATUS_OFFLINE -> {
                statusImageResId = if (Util.isDarkMode(context)) {
                    when (where) {
                        StatusIconLocation.STANDARD -> R.drawable.ic_offline_dark_standard
                        StatusIconLocation.DRAWER -> R.drawable.ic_offline_dark_drawer
                        StatusIconLocation.APPBAR -> R.drawable.ic_offline_dark_appbar
                    }
                } else {
                    R.drawable.ic_offline_light
                }
            }

            MegaChatApi.STATUS_INVALID -> {
                // Do nothing, let statusImageResId be 0.
            }
            else -> {
                // Do nothing, let statusImageResId be 0.
            }
        }

        return statusImageResId
    }

    /**
     * Sets the contact status icon and status text
     *
     * @param userStatus       contact's status
     * @param contactStateIcon view in which the status icon has to be set
     * @param contactStateText view in which the status text has to be set
     * @param where            The status icon image resource is different based on the place where it's placed.
     */
    @JvmStatic
    fun setContactStatus(
        userStatus: Int,
        contactStateIcon: ImageView?,
        contactStateText: TextView?,
        where: StatusIconLocation,
    ) {
        val app = MegaApplication.getInstance()
        setContactStatus(userStatus, contactStateIcon, where)

        if (contactStateText == null) {
            return
        }

        contactStateText.visibility = View.VISIBLE

        when (userStatus) {
            MegaChatApi.STATUS_ONLINE ->
                contactStateText.text = app.getString(R.string.online_status)

            MegaChatApi.STATUS_AWAY ->
                contactStateText.text = app.getString(R.string.away_status)

            MegaChatApi.STATUS_BUSY ->
                contactStateText.text = app.getString(R.string.busy_status)

            MegaChatApi.STATUS_OFFLINE ->
                contactStateText.text = app.getString(R.string.offline_status)

            MegaChatApi.STATUS_INVALID -> contactStateText.visibility = View.GONE
            else -> contactStateText.visibility = View.GONE
        }
    }

    /**
     * If the contact has last green, sets is as status text
     *
     * @param context          current Context
     * @param userStatus       contact's status
     * @param lastGreen        contact's last green
     * @param contactStateText view in which the last green has to be set
     */
    @JvmStatic
    fun setContactLastGreen(
        context: Context,
        userStatus: Int,
        lastGreen: String?,
        contactStateText: MarqueeTextView?,
    ) {
        if (contactStateText == null || isTextEmpty(lastGreen)) {
            return
        }

        if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
            contactStateText.text = lastGreen
            contactStateText.isMarqueeIsNecessary(context)
        }
    }

    /**
     * Method for obtaining the AudioFocusRequest when get the focus audio.
     *
     * @param listener  The listener.
     * @param focusType Type of focus.
     * @return The AudioFocusRequest.
     */
    @JvmStatic
    fun getRequest(
        listener: AudioManager.OnAudioFocusChangeListener,
        focusType: Int,
    ): AudioFocusRequest {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        return AudioFocusRequest.Builder(focusType)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(true)
            .setWillPauseWhenDucked(true)
            .setOnAudioFocusChangeListener(listener)
            .build()
    }

    /**
     * Knowing if permits have been successfully got.
     *
     * @return True, if it has been successful. False, if not.
     */
    @JvmStatic
    fun getAudioFocus(
        audioManager: AudioManager?,
        listener: AudioManager.OnAudioFocusChangeListener?,
        request: AudioFocusRequest?,
        focusType: Int,
        streamType: Int,
    ): Boolean {
        if (audioManager == null) {
            Timber.w("Audio Manager is NULL")
            return false
        }

        if (request == null) {
            Timber.w("Audio Focus Request is NULL")
            return false
        }
        val focusRequest = audioManager.requestAudioFocus(request)
        return focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * Method for leaving the audio focus.
     */
    @JvmStatic
    fun abandonAudioFocus(
        listener: AudioManager.OnAudioFocusChangeListener?,
        audioManager: AudioManager?,
        request: AudioFocusRequest?,
    ) {
        if (request != null) {
            audioManager!!.abandonAudioFocusRequest(request)
        }
    }

    /**
     * Method for obtaining the title of a MegaChatRoom.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    @JvmStatic
    fun getTitleChat(chat: MegaChatRoom?): String {
        if (chat == null) {
            Timber.e("chat is null")
            return ""
        }

        return chat.title
    }

    /**
     * Method for obtaining the title of a MegaChatListItem.
     *
     * @param chat The chat room.
     * @return String with the title.
     */
    @JvmStatic
    fun getTitleChat(chat: MegaChatListItem?): String {
        if (chat == null) {
            Timber.e("chat is null")
            return ""
        }

        return chat.title
    }

    /**
     * Method to know if the chat notifications are activated or deactivated.
     *
     * @return The type of mute.
     */
    @JvmStatic
    fun getGeneralNotification(): String {
        val app = MegaApplication.getInstance()
        val pushNotificationSettings = MegaApplication.getPushNotificationSettingManagement().pushNotificationSetting

        if (!pushNotificationSettings.isGlobalChatsDndEnabled || pushNotificationSettings.globalChatsDnd == -1L) {
            var chatSettings = app.dbH.chatSettings
            if (chatSettings == null) {
                chatSettings = ChatSettings()
                app.dbH.chatSettings = chatSettings
            }

            return Constants.NOTIFICATIONS_ENABLED
        }

        if (pushNotificationSettings.globalChatsDnd == 0L) {
            return Constants.NOTIFICATIONS_DISABLED
        }

        return Constants.NOTIFICATIONS_DISABLED_X_TIME
    }

    /**
     * Method to display a dialog to mute a specific chat.
     *
     * @param context Context of Activity.
     * @param chatId  Chat ID.
     */
    @JvmStatic
    fun createMuteNotificationsAlertDialogOfAChat(context: Activity, chatId: Long) {
        val chats = ArrayList<MegaChatListItem>()
        val chat = MegaApplication.getInstance().megaChatApi.getChatListItem(chatId)
        if (chat != null) {
            chats.add(chat)
            createMuteNotificationsChatAlertDialog(context, chats)
        }
    }

    /**
     * Method to display a dialog to mute a list of chats.
     *
     * @param context Context of Activity.
     * @param chatIds Chat IDs.
     */
    @JvmStatic
    fun createMuteNotificationsAlertDialogOfChats(context: Activity, chatIds: List<Long>) {
        val chats = ArrayList(
            chatIds.mapNotNull { chatId ->
                MegaApplication.getInstance().megaChatApi.getChatListItem(chatId)
            }
        )
        if (chats.isNotEmpty()) {
            createMuteNotificationsChatAlertDialog(context, chats)
        }
    }


    /**
     * Method to display a dialog to mute general chat notifications or several specific chats.
     *
     * @param context Context of Activity.
     * @param chats   Chats. If the chats is null, it's for the general chats notifications.
     */
    @JvmStatic
    fun createMuteNotificationsChatAlertDialog(context: Activity, chats: ArrayList<MegaChatListItem>?) {
        val dialogBuilder = MaterialAlertDialogBuilder(context)
        if (chats == null) {
            val view = context.layoutInflater.inflate(R.layout.title_mute_notifications, null)
            dialogBuilder.setCustomTitle(view)
        } else {
            dialogBuilder.setTitle(
                if (chats[0].isMeeting)
                    context.getString(R.string.meetings_mute_notifications_dialog_title)
                else
                    context.getString(R.string.title_dialog_mute_chatroom_notifications)
            )
        }

        val isUntilThisMorning = isUntilThisMorning()
        val optionUntil = if (chats != null)
            context.getString(R.string.mute_chatroom_notification_option_forever)
        else
            (if (isUntilThisMorning) context.getString(R.string.mute_chatroom_notification_option_until_this_morning)
            else context.getString(R.string.mute_chatroom_notification_option_until_tomorrow_morning))

        val optionSelected = if (chats != null)
            Constants.NOTIFICATIONS_DISABLED
        else
            (if (isUntilThisMorning) Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING
            else Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING)

        val itemClicked = AtomicReference<Int>()

        val stringsArray = ArrayList<String>()
        stringsArray.add(0, removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30)))
        stringsArray.add(1, removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1)))
        stringsArray.add(2, removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6)))
        stringsArray.add(3, removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24)))
        stringsArray.add(4, optionUntil)

        val itemsAdapter = ArrayAdapter(context, R.layout.checked_text_view_dialog_button, stringsArray)
        val listView = ListView(context)
        listView.adapter = itemsAdapter

        dialogBuilder.setSingleChoiceItems(itemsAdapter, Constants.INVALID_POSITION) { dialog, item ->
            itemClicked.set(item)
            (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
        }

        dialogBuilder.setPositiveButton(context.getString(mega.privacy.android.shared.resources.R.string.general_ok)) { dialog, _ ->
            var chatIds: ArrayList<Long>? = null
            if (chats != null) {
                chatIds = ArrayList()
                for (i in chats.indices) {
                    val chat = chats[i]
                    if (chat != null) {
                        chatIds.add(chat.chatId)
                    }
                }
            }

            MegaApplication.getPushNotificationSettingManagement()
                .controlMuteNotifications(
                    context,
                    getTypeMute(itemClicked.get(), optionSelected),
                    chatIds
                )
            dialog.dismiss()
        }
        dialogBuilder.setNegativeButton(context.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button)) { dialog, _ -> dialog.dismiss() }

        val muteDialog = dialogBuilder.create()
        muteDialog.show()
        muteDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
    }

    /**
     * Method for getting the string depending on the selected mute option.
     *
     * @param option The selected mute option.
     * @return The appropriate string.
     */
    @JvmStatic
    fun getMutedPeriodString(option: String): String? {
        val context = MegaApplication.getInstance().baseContext
        return when (option) {
            Constants.NOTIFICATIONS_30_MINUTES ->
                removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_minutes, 30, 30))

            Constants.NOTIFICATIONS_1_HOUR ->
                removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_hours, 1, 1))

            Constants.NOTIFICATIONS_6_HOURS ->
                removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_hours, 6, 6))

            Constants.NOTIFICATIONS_24_HOURS ->
                removeFormatPlaceholder(context.resources.getQuantityString(R.plurals.plural_call_ended_messages_hours, 24, 24))

            else -> null
        }
    }

    /**
     * Method for getting the selected mute option depending on the selected item.
     *
     * @param itemClicked    The selected item.
     * @param optionSelected The right choice when you select the fifth option.
     * @return The right mute option.
     */
    private fun getTypeMute(itemClicked: Int, optionSelected: String): String =
        when (itemClicked) {
            0 -> Constants.NOTIFICATIONS_30_MINUTES
            1 -> Constants.NOTIFICATIONS_1_HOUR
            2 -> Constants.NOTIFICATIONS_6_HOURS
            3 -> Constants.NOTIFICATIONS_24_HOURS
            4 -> optionSelected
            else -> Constants.NOTIFICATIONS_ENABLED
        }

    /**
     * Method to mute a specific chat or general notifications chat for a specific period of time.
     *
     * @param context    Context of Activity.
     * @param muteOption The selected mute option.
     */
    @JvmStatic
    fun muteChat(context: Context?, muteOption: String?) {
        ChatController(context).muteChat(muteOption)
    }

    /**
     * Method to checking when chat notifications are enabled and update the UI elements.
     *
     * @param chatHandle            Chat ID.
     * @param notificationsSwitch   The MegaSwitch.
     * @param notificationsSubTitle The TextView with the info.
     */
    @JvmStatic
    fun checkSpecificChatNotifications(
        chatHandle: Long,
        notificationsSwitch: MegaSwitch,
        notificationsSubTitle: TextView,
        context: Context,
    ) {
        updateSwitchButton(chatHandle, notificationsSwitch, notificationsSubTitle, context)
    }

    /**
     * Method to update the switch element related to the notifications of a specific chat.
     *
     * @param chatId                The chat ID.
     * @param notificationsSwitch   The MegaSwitch.
     * @param notificationsSubTitle The TextView with the info.
     */
    private fun updateSwitchButton(
        chatId: Long,
        notificationsSwitch: MegaSwitch,
        notificationsSubTitle: TextView,
        context: Context,
    ) {
        val push = MegaApplication.getPushNotificationSettingManagement().pushNotificationSetting

        if (push.isChatDndEnabled(chatId)) {
            notificationsSwitch.setChecked(false)
            val timestampMute = push.getChatDnd(chatId)
            notificationsSubTitle.visibility = View.VISIBLE
            notificationsSubTitle.text = if (timestampMute == 0L)
                MegaApplication.getInstance().getString(R.string.mute_chatroom_notification_option_off)
            else
                getCorrectStringDependingOnOptionSelected(timestampMute, context)
        } else {
            notificationsSwitch.setChecked(true)
            notificationsSubTitle.visibility = View.GONE
        }
    }

    /**
     * Gets the user's online status.
     *
     * @param userHandle handle of the user
     * @return The user's status.
     */
    @JvmStatic
    fun getUserStatus(userHandle: Long): Int =
        if (isContact(userHandle))
            MegaApplication.getInstance().megaChatApi.getUserOnlineStatus(userHandle)
        else
            MegaChatApi.STATUS_INVALID

    /**
     * Method for obtaining the contact status bitmap.
     *
     * @param userStatus The contact status.
     * @return The final bitmap.
     */
    @JvmStatic
    fun getStatusBitmap(userStatus: Int): Bitmap? {
        val resources = MegaApplication.getInstance().baseContext.resources
        val isDarkMode = Util.isDarkMode(MegaApplication.getInstance())
        return when (userStatus) {
            MegaChatApi.STATUS_ONLINE ->
                BitmapFactory.decodeResource(
                    resources,
                    if (isDarkMode) R.drawable.ic_online_dark_standard
                    else R.drawable.ic_online_light
                )

            MegaChatApi.STATUS_AWAY ->
                BitmapFactory.decodeResource(
                    resources,
                    if (isDarkMode) R.drawable.ic_away_dark_standard
                    else R.drawable.ic_away_light
                )

            MegaChatApi.STATUS_BUSY ->
                BitmapFactory.decodeResource(
                    resources,
                    if (isDarkMode) R.drawable.ic_busy_dark_standard
                    else R.drawable.ic_busy_light
                )

            MegaChatApi.STATUS_OFFLINE ->
                BitmapFactory.decodeResource(
                    resources,
                    if (isDarkMode) R.drawable.ic_offline_dark_standard
                    else R.drawable.ic_offline_light
                )

            MegaChatApi.STATUS_INVALID -> null
            else -> null
        }
    }

    /**
     * Gets retention time for a particular chat.
     *
     * @param idChat The chat ID.
     * @return The retention time in seconds.
     */
    @JvmStatic
    fun getUpdatedRetentionTimeFromAChat(idChat: Long): Long {
        val chat = MegaApplication.getInstance().megaChatApi.getChatRoom(idChat)
        if (chat != null) {
            return chat.retentionTime
        }

        return Constants.DISABLED_RETENTION_TIME.toLong()
    }

    /**
     * Method for getting the appropriate String from the seconds of rentention time.
     *
     * @param seconds The retention time in seconds
     * @return The right text
     */
    @Deprecated("Use RetentionTimeUpdatedMessageView.getRetentionTimeString instead.")
    @JvmStatic
    fun transformSecondsInString(seconds: Long): String {
        if (seconds == Constants.DISABLED_RETENTION_TIME.toLong())
            return ""

        val hours = seconds % Constants.SECONDS_IN_HOUR
        val days = seconds % Constants.SECONDS_IN_DAY
        val weeks = seconds % Constants.SECONDS_IN_WEEK
        val months = seconds % Constants.SECONDS_IN_MONTH_30
        val years = seconds % Constants.SECONDS_IN_YEAR

        if (years == 0L) {
            return MegaApplication.getInstance().baseContext.resources.getString(R.string.subtitle_properties_manage_chat_label_year)
        }

        if (months == 0L) {
            val month = (seconds / Constants.SECONDS_IN_MONTH_30).toInt()
            return MegaApplication.getInstance().baseContext.resources.getQuantityString(R.plurals.subtitle_properties_manage_chat_label_months, month, month)
        }

        if (weeks == 0L) {
            val week = (seconds / Constants.SECONDS_IN_WEEK).toInt()
            return MegaApplication.getInstance().baseContext.resources.getQuantityString(R.plurals.subtitle_properties_manage_chat_label_weeks, week, week)
        }

        if (days == 0L) {
            val day = (seconds / Constants.SECONDS_IN_DAY).toInt()
            return MegaApplication.getInstance().baseContext.resources.getQuantityString(R.plurals.label_time_in_days_full, day, day)
        }

        if (hours == 0L) {
            val hour = (seconds / Constants.SECONDS_IN_HOUR).toInt()
            return MegaApplication.getInstance().baseContext.resources.getQuantityString(R.plurals.subtitle_properties_manage_chat_label_hours, hour, hour)
        }

        return ""
    }

    /**
     * Method for updating the Time retention layout.
     *
     * @param time The retention time in seconds.
     */
    @JvmStatic
    fun updateRetentionTimeLayout(retentionTimeText: TextView, time: Long, context: Context) {
        val timeFormatted = transformSecondsInString(time)
        if (isTextEmpty(timeFormatted)) {
            retentionTimeText.visibility = View.GONE
        } else {
            val subtitleText = context.getString(R.string.subtitle_properties_manage_chat) + " " + timeFormatted
            retentionTimeText.text = subtitleText
            retentionTimeText.visibility = View.VISIBLE
        }
    }

    /**
     * Authorizes the node if the chat is on preview mode.
     *
     * @param node        Node to authorize.
     * @param megaChatApi MegaChatApiAndroid instance.
     * @param megaApi     MegaApiAndroid instance.
     * @param chatId      Chat identifier to check.
     * @return The authorized node if preview, same node otherwise.
     */
    @JvmStatic
    fun authorizeNodeIfPreview(
        node: MegaNode,
        megaChatApi: MegaChatApiAndroid,
        megaApi: MegaApiAndroid,
        chatId: Long,
    ): MegaNode {
        val chatRoom = megaChatApi.getChatRoom(chatId)

        if (chatRoom != null && chatRoom.isPreview) {
            val nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.authorizationToken)

            if (nodeAuthorized != null) {
                Timber.d("Authorized")
                return nodeAuthorized
            }
        }

        return node
    }

    /**
     * Remove an attachment message from chat.
     *
     * @param activity Android activity
     * @param chatId   chat id
     * @param message  chat message
     */
    @JvmStatic
    fun removeAttachmentMessage(activity: Activity, chatId: Long, message: MegaChatMessage?) {
        MaterialAlertDialogBuilder(activity)
            .setMessage(activity.getString(R.string.confirmation_delete_one_attachment))
            .setPositiveButton(activity.getString(R.string.context_remove)) { _, _ ->
                ChatController(activity).deleteMessage(message, chatId)
                activity.finish()
            }
            .setNegativeButton(activity.getString(mega.privacy.android.shared.resources.R.string.general_dialog_cancel_button), null)
            .show()
    }

    /**
     * Launches an Intent to open TextFileEditorActivity.
     *
     * @param context Current context.
     * @param msgId   Message identifier.
     * @param chatId  Chat identifier.
     */
    @JvmStatic
    fun manageTextFileIntent(context: Context, msgId: Long, chatId: Long) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MegaNavigatorEntryPoint::class.java
        ).megaNavigator.openTextEditor(context, OpenTextEditorParams.Chat(chatId, msgId))
    }

    /**
     * Method to find out if I am participating in a chat room
     *
     * @param chatId The chat ID
     * @return True, if I am joined to the chat. False, if not
     */
    @JvmStatic
    fun amIParticipatingInAChat(chatId: Long): Boolean {
        val chatRoom = MegaApplication.getInstance().megaChatApi.getChatRoom(chatId)
            ?: return false

        if (chatRoom.isPreview) {
            return false
        }

        val myPrivileges = chatRoom.ownPrivilege
        return myPrivileges == MegaChatRoom.PRIV_RO || myPrivileges == MegaChatRoom.PRIV_STANDARD || myPrivileges == MegaChatRoom.PRIV_MODERATOR
    }

    /**
     * Method to get the initial state of megaChatApi and, if necessary, initiates it.
     *
     * @param session User session
     */
    @JvmStatic
    fun initMegaChatApi(session: String?) {
        initMegaChatApi(session, null)
    }

    /**
     * Method to get the initial state of megaChatApi and, if necessary, initiates it.
     *
     * @param session  User session
     * @param listener MegaChat listener for logout request.
     */
    @JvmStatic
    fun initMegaChatApi(session: String?, listener: MegaChatRequestListenerInterface?) {
        val megaChatApi = MegaApplication.getInstance().megaChatApi

        var state = megaChatApi.initState
        if (state == MegaChatApi.INIT_NOT_DONE || state == MegaChatApi.INIT_ERROR) {
            state = megaChatApi.init(session)
            Timber.d("result of init ---> %s", state)
            when (state) {
                MegaChatApi.INIT_NO_CACHE -> Timber.d("INIT_NO_CACHE")
                MegaChatApi.INIT_ERROR -> {
                    Timber.d("INIT_ERROR")
                    if (listener != null) {
                        megaChatApi.logout(listener)
                    } else {
                        megaChatApi.logout()
                    }
                }
                else -> Timber.d("Chat correctly initialized")
            }
        }
    }

    /**
     * Method to check if all user's contacts are participants of the chat.
     *
     * @param chatId Chat id
     * @return True if all user's contacts are participants of the chat room or false otherwise.
     */
    @JvmStatic
    fun areAllMyContactsChatParticipants(chatId: Long): Boolean {
        val chat = MegaApplication.getInstance().megaChatApi.getChatRoom(chatId)
        return areAllMyContactsChatParticipants(chat)
    }

    /**
     * Method to check if all user's contacts are participants of the chat. use [AreAllParticipantsInContactUseCase] instead
     *
     * @param chatRoom MegaChatRoom to check
     * @return True if all user's contacts are participants of the chat room or false otherwise.
     */
    @Deprecated("Use AreAllParticipantsInContactUseCase instead")
    @JvmStatic
    fun areAllMyContactsChatParticipants(chatRoom: MegaChatRoom?): Boolean {
        if (chatRoom == null) {
            return false
        }

        val contacts = MegaApplication.getInstance().megaApi.contacts
        val peerCount = chatRoom.peerCount
        var areAllMyContactsChatParticipants = true

        for (i in contacts.indices) {
            if (contacts[i].visibility == VISIBILITY_VISIBLE) {
                var contactIsParticipant = false
                for (j in 0 until peerCount) {
                    if (contacts[i].handle == chatRoom.getPeerHandle(j)) {
                        contactIsParticipant = true
                        break
                    }
                }
                if (!contactIsParticipant) {
                    areAllMyContactsChatParticipants = false
                    break
                }
            }
        }

        return areAllMyContactsChatParticipants
    }
}
