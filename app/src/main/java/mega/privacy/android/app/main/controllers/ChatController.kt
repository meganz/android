package mega.privacy.android.app.main.controllers

import android.content.Context
import android.content.Intent
import android.text.Html
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.settingsActivities.ChatNotificationsPreferencesActivity
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile
import mega.privacy.android.app.utils.ChatUtil.getMegaChatMessage
import mega.privacy.android.app.utils.ChatUtil.getMutedPeriodString
import mega.privacy.android.app.utils.Constants.ACTION_FORWARD_MESSAGES
import mega.privacy.android.app.utils.Constants.ID_CHAT_FROM
import mega.privacy.android.app.utils.Constants.ID_MESSAGES
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING
import mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CHAT
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER
import mega.privacy.android.app.utils.ContactUtil.getContactEmailDB
import mega.privacy.android.app.utils.ContactUtil.getContactNameDB
import mega.privacy.android.app.utils.ContactUtil.getFirstNameDB
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MeetingUtil
import mega.privacy.android.app.utils.TimeUtils.getCorrectStringDependingOnCalendar
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.app.utils.Util.toCDATAOrNull
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import mega.privacy.android.data.qualifier.MegaApi
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

class ChatController @Inject constructor(
    @ApplicationContext private val context: Context,
    @MegaApi private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val dbH: DatabaseHandler,
) {

    fun deleteMessages(messages: ArrayList<MegaChatMessage>, chat: MegaChatRoom) {
        Timber.d("Messages to delete: %s", messages.size)
        for (i in messages.indices) {
            deleteMessage(messages[i], chat.chatId)
        }
    }

    fun deleteMessage(message: MegaChatMessage, chatId: Long) {
        Timber.d("Message : %d, Chat ID: %d", message.msgId, chatId)
        val messageToDelete: MegaChatMessage?
        if (message.type == MegaChatMessage.TYPE_NODE_ATTACHMENT || message.type == MegaChatMessage.TYPE_VOICE_CLIP) {
            Timber.d("Delete node attachment message or voice clip message")
            if (message.type == MegaChatMessage.TYPE_VOICE_CLIP && message.megaNodeList != null && message.megaNodeList
                    .size() > 0 && message.megaNodeList.get(0) != null
            ) {
                deleteOwnVoiceClip(context, message.megaNodeList.get(0).name)
            }
            messageToDelete = megaChatApi.revokeAttachmentMessage(chatId, message.msgId)
        } else {
            Timber.d("Delete normal message with status = %s", message.status)
            if (message.status == MegaChatMessage.STATUS_SENDING && message.msgId == MegaApiJava.INVALID_HANDLE) {
                messageToDelete = megaChatApi.deleteMessage(chatId, message.tempId)
            } else {
                messageToDelete = megaChatApi.deleteMessage(chatId, message.msgId)
            }
        }

        if (messageToDelete == null) {
            Timber.d("The message cannot be deleted")
        }
    }

    /**
     * Method to silence notifications for all chats or for a specific chat.
     * 
     * @param option The selected mute option.
     */
    fun muteChat(option: String) {
        if (context is ChatNotificationsPreferencesActivity) return

        when (option) {
            NOTIFICATIONS_ENABLED -> showSnackbar(
                context, context.getString(R.string.success_unmuting_a_chat)
            )

            NOTIFICATIONS_DISABLED -> showSnackbar(
                context, context.getString(R.string.notifications_are_already_muted)
            )

            NOTIFICATIONS_DISABLED_UNTIL_THIS_MORNING, NOTIFICATIONS_DISABLED_UNTIL_TOMORROW_MORNING -> showSnackbar(
                context, getCorrectStringDependingOnCalendar(option, context)
            )

            else -> {
                val text = getMutedPeriodString(option)
                if (!isTextEmpty(text)) {
                    showSnackbar(
                        context,
                        context.getString(R.string.success_muting_a_chat_for_specific_time, text)
                    )
                }
            }
        }
    }

    private fun isTextEmpty(text: String?): Boolean {
        return text == null || text.trim { it <= ' ' }.isEmpty()
    }

    fun createManagementString(message: MegaChatMessage?, chatRoom: MegaChatRoom): String? {
        if (message == null) {
            Timber.w("Null MegaChatMessage")
            return null
        }

        Timber.d("MessageID: %d, Chat ID: %d", message.msgId, chatRoom.chatId)
        val userHandle = message.userHandle

        if (message.type == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) {
            Timber.d("ALTER PARTICIPANT MESSAGE!")

            if (megaApi.myUser != null && message.handleOfAction == megaApi.myUser
                    ?.handle
            ) {
                Timber.d("Me alter participant")

                val builder = StringBuilder()

                val privilege = message.privilege
                Timber.d("Privilege of me: %s", privilege)
                val textToShow: String?
                val fullNameAction = getParticipantFullName(message.userHandle)

                if (privilege != MegaChatRoom.PRIV_RM) {
                    Timber.d("I was added")
                    textToShow = String.format(
                        context.getString(R.string.non_format_message_add_participant),
                        megaChatApi.myFullname,
                        fullNameAction
                    )
                } else {
                    Timber.d("I was removed or left")
                    if (message.userHandle == message.handleOfAction) {
                        Timber.d("I left the chat")
                        textToShow = String.format(
                            context.getString(R.string.non_format_message_participant_left_group_chat),
                            megaChatApi.myFullname
                        )
                    } else {
                        textToShow = String.format(
                            context.getString(R.string.non_format_message_remove_participant),
                            megaChatApi.myFullname,
                            fullNameAction
                        )
                    }
                }

                builder.append(textToShow)
                return builder.toString()
            } else {
                Timber.d("CONTACT Message type ALTER PARTICIPANTS")

                val privilege = message.privilege
                Timber.d("Privilege of the user: %s", privilege)

                val fullNameTitle = getParticipantFullName(message.handleOfAction)

                val builder = StringBuilder()

                var textToShow = ""
                if (privilege != MegaChatRoom.PRIV_RM) {
                    Timber.d("Participant was added")
                    if (megaApi.myUser != null && message.userHandle == megaApi.myUser
                            ?.handle
                    ) {
                        Timber.d("By me")
                        textToShow = String.format(
                            context.getString(R.string.non_format_message_add_participant),
                            fullNameTitle,
                            megaChatApi.myFullname
                        )
                    } else {
                        Timber.d("By other")
                        val fullNameAction = getParticipantFullName(message.userHandle)
                        textToShow = String.format(
                            context.getString(R.string.non_format_message_add_participant),
                            fullNameTitle,
                            fullNameAction
                        )
                    }
                } //END participant was added
                else {
                    Timber.d("Participant was removed or left")
                    if (megaApi.myUser != null && message.userHandle == megaApi.myUser
                            ?.handle
                    ) {
                        textToShow = String.format(
                            context.getString(R.string.non_format_message_remove_participant),
                            fullNameTitle,
                            megaChatApi.myFullname
                        )
                    } else {
                        if (message.userHandle == message.handleOfAction) {
                            Timber.d("The participant left the chat")

                            textToShow = String.format(
                                context.getString(R.string.non_format_message_participant_left_group_chat),
                                fullNameTitle
                            )
                        } else {
                            Timber.d("The participant was removed")
                            val fullNameAction = getParticipantFullName(message.userHandle)
                            textToShow = String.format(
                                context.getString(R.string.non_format_message_remove_participant),
                                fullNameTitle,
                                fullNameAction
                            )
                        }
                    }
                } //END participant removed


                builder.append(textToShow)
                return builder.toString()
            } //END CONTACT MANAGEMENT MESSAGE
        } else if (message.type == MegaChatMessage.TYPE_PRIV_CHANGE) {
            val privilege = message.privilege
            Timber.d("Privilege of the user: %s", privilege)

            val builder = StringBuilder()
            val participantsNameWhoMadeTheAction =
                if (megaApi.myUser != null && message.handleOfAction == megaApi.myUser
                        ?.handle
                ) megaChatApi.myFullname else getParticipantFullName(message.handleOfAction)
            val participantsNameWhosePermissionsWereChanged =
                if (megaApi.myUser != null && message.userHandle == megaApi.myUser
                        ?.handle
                ) megaChatApi.myFullname else getParticipantFullName(message.userHandle)

            var textToShow = ""
            when (privilege) {
                MegaChatRoom.PRIV_MODERATOR -> textToShow = context.getString(
                    R.string.chat_chats_list_last_message_permissions_changed_to_host,
                    participantsNameWhoMadeTheAction,
                    participantsNameWhosePermissionsWereChanged
                )

                MegaChatRoom.PRIV_STANDARD -> textToShow = context.getString(
                    R.string.chat_chats_list_last_message_permissions_changed_to_standard,
                    participantsNameWhoMadeTheAction,
                    participantsNameWhosePermissionsWereChanged
                )

                MegaChatRoom.PRIV_RO -> textToShow = context.getString(
                    R.string.chat_chats_list_last_message_permissions_changed_to_read_only,
                    participantsNameWhoMadeTheAction,
                    participantsNameWhosePermissionsWereChanged
                )
            }

            builder.append(textToShow)
            return builder.toString()
        } else {
            Timber.d("Other type of messages")
            //OTHER TYPE OF MESSAGES
            if (megaApi.myUser != null && megaApi.myUser
                    ?.handle == message.userHandle
            ) {
                Timber.d("MY message ID: %s", message.msgId)

                val builder = StringBuilder()
                if (message.type == MegaChatMessage.TYPE_NORMAL) {
                    Timber.d("Message type NORMAL")

                    builder.append(context.getString(R.string.chat_last_message_sender_me))
                        .append(": ")
                    var messageContent: String? = ""
                    if (message.content != null) {
                        messageContent = message.content
                    }

                    if (message.isEdited) {
                        Timber.d("Message is edited")
                        val textToShow =
                            messageContent + " " + context.getString(R.string.edited_message_text)
                        builder.append(textToShow)
                        return builder.toString()
                    } else if (message.isDeleted) {
                        Timber.d("Message is deleted")

                        val textToShow = context.getString(R.string.text_deleted_message)
                        builder.append(textToShow)
                        return builder.toString()
                    } else {
                        builder.append(messageContent)
                        return builder.toString()
                    }
                } else if (message.type == MegaChatMessage.TYPE_TRUNCATE) {
                    Timber.d("Message type TRUNCATE")

                    var textToShow = String.format(
                        context.getString(R.string.history_cleared_by),
                        megaChatApi.myFullname.toCDATAOrNull()
                    )
                    try {
                        textToShow = textToShow.replace("[A]", "<font color=\'#060000\'>")
                        textToShow = textToShow.replace("[/A]", "</font>")
                        textToShow = textToShow.replace("[B]", "<font color=\'#00BFA5\'>")
                        textToShow = textToShow.replace("[/B]", "</font>")
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                    val result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
                    builder.append(result)
                    return builder.toString()
                } else if (message.type == MegaChatMessage.TYPE_CHAT_TITLE) {
                    Timber.d("Message type TITLE CHANGE - Message ID: %s", message.msgId)

                    val messageContent = message.content
                    val textToShow = String.format(
                        context.getString(R.string.non_format_change_title_messages),
                        megaChatApi.myFullname,
                        messageContent
                    )
                    builder.append(textToShow)
                    return builder.toString()
                } else if (message.type == MegaChatMessage.TYPE_CONTAINS_META) {
                    builder.append(context.getString(R.string.chat_last_message_sender_me))
                        .append(": ")
                    val meta = message.containsMeta
                    if (meta != null) {
                        when (meta.type) {
                            MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW -> {
                                val text = meta.richPreview.text
                                builder.append(text)
                                return builder.toString()
                            }

                            MegaChatContainsMeta.CONTAINS_META_GEOLOCATION -> {
                                val text = context.getString(R.string.title_geolocation_message)
                                builder.append(text)
                                return builder.toString()
                            }

                            MegaChatContainsMeta.CONTAINS_META_GIPHY -> {
                                val text = message.containsMeta.giphy.title
                                builder.append(text)
                                return builder.toString()
                            }
                        }
                    }

                    return ""
                } else if (message.type == MegaChatMessage.TYPE_CALL_STARTED) {
                    builder.append(context.getString(R.string.chat_last_message_sender_me))
                        .append(": ")
                    val textToShow =
                        MeetingUtil.getAppropriateStringForCallStarted(context).toString()
                    builder.append(textToShow)
                    return builder.toString()
                } else if (message.type == MegaChatMessage.TYPE_CALL_ENDED) {
                    builder.append(context.getString(R.string.chat_last_message_sender_me))
                        .append(": ")
                    var textToShow = ""
                    when (message.termCode) {
                        MegaChatMessage.END_CALL_REASON_BY_MODERATOR, MegaChatMessage.END_CALL_REASON_ENDED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallEnded(
                                chatRoom, message.duration.toLong(), context
                            ).toString()

                        MegaChatMessage.END_CALL_REASON_REJECTED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallRejected(context).toString()

                        MegaChatMessage.END_CALL_REASON_NO_ANSWER -> textToShow =
                            MeetingUtil.getAppropriateStringForCallNoAnswered(
                                message.userHandle, megaChatApi.myUserHandle, context
                            ).toString()

                        MegaChatMessage.END_CALL_REASON_FAILED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallFailed(context).toString()

                        MegaChatMessage.END_CALL_REASON_CANCELLED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallCancelled(
                                message.userHandle, megaChatApi.myUserHandle, context
                            ).toString()
                    }

                    builder.append(textToShow)
                    return builder.toString()
                } else {
                    return ""
                }
            } else {
                Timber.d("Contact message!")

                val fullNameTitle = getParticipantFullName(userHandle)
                val builder = StringBuilder()

                if (message.type == MegaChatMessage.TYPE_NORMAL) {
                    builder.append(fullNameTitle).append(": ")
                    var messageContent: String? = ""
                    if (message.content != null) {
                        messageContent = message.content
                    }

                    if (message.isEdited) {
                        Timber.d("Message is edited")

                        val textToShow =
                            messageContent + " " + context.getString(R.string.edited_message_text)
                        builder.append(textToShow)
                        return builder.toString()
                    } else if (message.isDeleted) {
                        Timber.d("Message is deleted")
                        var textToShow = ""
                        if (chatRoom.isGroup) {
                            textToShow = String.format(
                                context.getString(R.string.non_format_text_deleted_message_by),
                                fullNameTitle
                            )
                        } else {
                            textToShow = context.getString(R.string.text_deleted_message)
                        }

                        builder.append(textToShow)
                        return builder.toString()
                    } else {
                        builder.append(messageContent)
                        return builder.toString()
                    }
                } else if (message.type == MegaChatMessage.TYPE_TRUNCATE) {
                    Timber.d("Message type TRUNCATE")

                    val textToShow = String.format(
                        context.getString(R.string.non_format_history_cleared_by), fullNameTitle
                    )
                    builder.append(textToShow)
                    return builder.toString()
                } else if (message.type == MegaChatMessage.TYPE_CHAT_TITLE) {
                    Timber.d("Message type CHANGE TITLE - Message ID: %s", message.msgId)

                    val messageContent = message.content

                    val textToShow = String.format(
                        context.getString(R.string.non_format_change_title_messages),
                        fullNameTitle,
                        messageContent
                    )
                    builder.append(textToShow)
                    return builder.toString()
                } else if (message.type == MegaChatMessage.TYPE_CONTAINS_META) {
                    builder.append(fullNameTitle).append(": ")
                    val meta = message.containsMeta
                    if (meta != null) {
                        when (meta.type) {
                            MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW -> {
                                val text = meta.richPreview.text
                                builder.append(text)
                                return builder.toString()
                            }

                            MegaChatContainsMeta.CONTAINS_META_GEOLOCATION -> {
                                val text = context.getString(R.string.title_geolocation_message)
                                builder.append(text)
                                return builder.toString()
                            }

                            MegaChatContainsMeta.CONTAINS_META_GIPHY -> {
                                val text = message.containsMeta.giphy.title
                                builder.append(text)
                                return builder.toString()
                            }
                        }
                    }
                    return ""
                } else if (message.type == MegaChatMessage.TYPE_CALL_STARTED) {
                    builder.append(fullNameTitle).append(": ")
                    val textToShow =
                        MeetingUtil.getAppropriateStringForCallStarted(context).toString()
                    builder.append(textToShow)
                    return builder.toString()
                } else if (message.type == MegaChatMessage.TYPE_CALL_ENDED) {
                    builder.append(fullNameTitle).append(": ")
                    var textToShow = ""
                    when (message.termCode) {
                        MegaChatMessage.END_CALL_REASON_BY_MODERATOR, MegaChatMessage.END_CALL_REASON_ENDED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallEnded(
                                chatRoom, message.duration.toLong(), context
                            ).toString()

                        MegaChatMessage.END_CALL_REASON_REJECTED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallRejected(context).toString()

                        MegaChatMessage.END_CALL_REASON_NO_ANSWER -> textToShow =
                            MeetingUtil.getAppropriateStringForCallNoAnswered(
                                message.userHandle, megaChatApi.myUserHandle, context
                            ).toString()

                        MegaChatMessage.END_CALL_REASON_FAILED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallFailed(context).toString()

                        MegaChatMessage.END_CALL_REASON_CANCELLED -> textToShow =
                            MeetingUtil.getAppropriateStringForCallCancelled(
                                message.userHandle, megaChatApi.myUserHandle, context
                            ).toString()
                    }

                    builder.append(textToShow)
                    return builder.toString()
                } else {
                    Timber.d("Message type: %s", message.type)
                    Timber.d("Message ID: %s", message.msgId)
                    return ""
                }
            }
        }
    }

    /**
     * Gets a partcipant's name (not contact).
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     * 
     * @param userHandle participant's identifier
     * @return The participant's name.
     */
    private fun getNonContactFirstName(userHandle: Long): String? {
        val nonContact = dbH.findNonContactByHandle(userHandle.toString() + "")
        if (nonContact == null) {
            return ""
        }

        var name = nonContact.firstName

        if (isTextEmpty(name)) {
            name = nonContact.lastName
        }

        if (isTextEmpty(name)) {
            name = nonContact.email
        }

        return name
    }

    /**
     * Gets a partcipant's full name (not contact).
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     * 
     * @param userHandle participant's identifier
     * @return The participant's full name.
     */
    private fun getNonContactFullName(userHandle: Long): String? {
        val nonContact = dbH.findNonContactByHandle(userHandle.toString() + "")
        if (nonContact == null) {
            return ""
        }

        var fullName = nonContact.fullName

        if (isTextEmpty(fullName)) {
            fullName = nonContact.email
        }

        return fullName
    }

    /**
     * Gets a partcipant's email (not contact).
     * 
     * @param userHandle participant's identifier
     * @return The participant's email.
     */
    private fun getNonContactEmail(userHandle: Long): String? {
        val nonContact = dbH.findNonContactByHandle(userHandle.toString() + "")
        return if (nonContact != null) nonContact.email else ""
    }

    val myFullName: String?
        get() {
            var fullName = megaChatApi.myFullname

            if (fullName != null) {
                if (fullName.isEmpty()) {
                    Timber.d("Put MY email as fullname")
                    val myEmail = megaChatApi.myEmail
                    val splitEmail: Array<String?> =
                        myEmail.split("[@._]".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    fullName = splitEmail[0]
                } else {
                    if (fullName.trim { it <= ' ' }.length <= 0) {
                        Timber.d("Put MY email as fullname")
                        val myEmail = megaChatApi.myEmail
                        val splitEmail: Array<String?> =
                            myEmail.split("[@._]".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        fullName = splitEmail[0]
                    }
                }
            } else {
                Timber.d("Put MY email as fullname")
                val myEmail = megaChatApi.myEmail
                val splitEmail: Array<String?> =
                    myEmail.split("[@._]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                fullName = splitEmail[0]
            }

            return fullName
        }

    fun importNode(idMessage: Long, idChat: Long, typeImport: Int) {
        Timber.d("Message ID: %d, Chat ID: %d", idMessage, idChat)
        val messages = ArrayList<AndroidMegaChatMessage?>()
        val m = getMegaChatMessage(context, megaChatApi, idChat, idMessage)

        if (m != null) {
            val aMessage = AndroidMegaChatMessage(m)
            messages.add(aMessage)
            importNodesFromAndroidMessages(messages, typeImport)
        } else {
            Timber.w("Message cannot be recovered - null")
        }
    }

    fun importNodesFromMessages(messages: ArrayList<MegaChatMessage>) {
        Timber.d("importNodesFromMessages")

        val intent = Intent(context, FileExplorerActivity::class.java)
        intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER)

        val longArray = messages.mapNotNull { it.msgId }.toLongArray()

        intent.putExtra("HANDLES_IMPORT_CHAT", longArray)

        if (context is NodeAttachmentHistoryActivity) {
            context.startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER)
        }
    }

    fun importNodesFromAndroidMessages(
        messages: ArrayList<AndroidMegaChatMessage?>,
        typeImport: Int,
    ) {
        Timber.d("importNodesFromAndroidMessages")

        val intent = Intent(context, FileExplorerActivity::class.java)
        intent.setAction(FileExplorerActivity.ACTION_PICK_IMPORT_FOLDER)

        val longArray = messages.mapNotNull { it?.message?.msgId }.toLongArray()

        intent.putExtra("HANDLES_IMPORT_CHAT", longArray)

        if (context is NodeAttachmentHistoryActivity) {
            context.startActivityForResult(intent, REQUEST_CODE_SELECT_IMPORT_FOLDER)
        }
    }

    fun forwardMessages(messagesSelected: ArrayList<MegaChatMessage>, idChat: Long) {
        Timber.d("Number of messages: %d, Chat ID: %d", messagesSelected.size, idChat)

        val idMessages = messagesSelected.mapNotNull { it.msgId }.toLongArray()

        val i = Intent(context, ChatExplorerActivity::class.java)
        i.putExtra(ID_MESSAGES, idMessages)
        i.putExtra(ID_CHAT_FROM, idChat)
        i.setAction(ACTION_FORWARD_MESSAGES)
        if (context is NodeAttachmentHistoryActivity) {
            context.startActivityForResult(i, REQUEST_CODE_SELECT_CHAT)
        }
    }

    fun authorizeNodeIfPreview(node: MegaNode?, chatRoom: MegaChatRoom?): MegaNode? {
        if (chatRoom != null && chatRoom.isPreview) {
            val nodeAuthorized = megaApi.authorizeChatNode(node, chatRoom.authorizationToken)
            if (nodeAuthorized != null) {
                Timber.d("Authorized")
                return nodeAuthorized
            }
        }
        Timber.d("NOT authorized")
        return node
    }

    val isInAnonymousMode: Boolean
        get() = megaChatApi.initState == MegaChatApi.INIT_ANONYMOUS

    fun isPreview(chatRoom: MegaChatRoom?): Boolean {
        if (chatRoom != null) {
            return chatRoom.isPreview
        }

        return false
    }

    /**
     * Stores in DB the user's attributes of a non contact.
     * 
     * @param peerHandle identifier of the user to save
     */
    fun setNonContactAttributesInDB(peerHandle: Long) {
        val dbH: DatabaseHandler = getInstance().dbH
        val megaChatApi = getInstance().getMegaChatApi()

        val firstName = megaChatApi.getUserFirstnameFromCache(peerHandle)
        if (!isTextEmpty(firstName)) {
            dbH.setNonContactFirstName(firstName, peerHandle.toString() + "")
        }

        val lastName = megaChatApi.getUserLastnameFromCache(peerHandle)
        if (!isTextEmpty(lastName)) {
            dbH.setNonContactLastName(lastName, peerHandle.toString() + "")
        }

        val email = megaChatApi.getUserEmailFromCache(peerHandle)
        if (!isTextEmpty(email)) {
            dbH.setNonContactEmail(email, peerHandle.toString() + "")
        }
    }

    /**
     * Use [GetParticipantFirstNameUseCase] instead.
     * Gets the participant's first name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a first name, it returns the first name.
     * If the participant has a last name, it returns the last name.
     * Otherwise, it returns the email.
     * 
     * @param userHandle participant's identifier
     * @return The participant's first name
     */
    @Deprecated("")
    fun getParticipantFirstName(userHandle: Long): String? {
        var firstName: String? = getFirstNameDB(userHandle)

        if (isTextEmpty(firstName)) {
            firstName = getNonContactFirstName(userHandle)
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserFirstnameFromCache(userHandle)
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserLastnameFromCache(userHandle)
        }

        if (isTextEmpty(firstName)) {
            firstName = megaChatApi.getUserEmailFromCache(userHandle)
        }

        return firstName
    }

    /**
     * Gets the participant's full name.
     * If the participant has an alias, it returns the alias.
     * If the participant has a full name, it returns the full name.
     * Otherwise, it returns the email.
     * 
     * @param handle participant's identifier
     * @return The participant's full name.
     */
    fun getParticipantFullName(handle: Long): String? {
        var fullName = getContactNameDB(handle)

        if (isTextEmpty(fullName)) {
            fullName = getNonContactFullName(handle)
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserFullnameFromCache(handle)
        }

        if (isTextEmpty(fullName)) {
            fullName = megaChatApi.getUserEmailFromCache(handle)
        }

        return fullName
    }

    /**
     * Gets the participant's email.
     * 
     * @param handle participant's identifier
     * @return The participant's email.
     */
    fun getParticipantEmail(handle: Long): String? {
        var email = getContactEmailDB(handle)

        if (isTextEmpty(email)) {
            email = getNonContactEmail(handle)
        }

        if (isTextEmpty(email)) {
            email = megaChatApi.getUserEmailFromCache(handle)
        }

        return email
    }

    companion object {
        /*
     * Delete a voice note from local storage
     */
        fun deleteOwnVoiceClip(mContext: Context?, nameFile: String?) {
            Timber.d("deleteOwnVoiceClip")
            val localFile = buildVoiceClipFile(nameFile)
            if (!FileUtil.isFileAvailable(localFile)) return
            localFile?.delete()
        }
    }
}
