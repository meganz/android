package mega.privacy.android.app.usecase.chat

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.utils.CacheFolderManager
import mega.privacy.android.app.utils.FileUtil
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatMessage.*
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * Main use case to delete a Mega Chat Message.
 *
 * @property megaChatApi    Mega Chat API needed to get message information.
 */
class DeleteChatMessageUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaChatApi: MegaChatApiAndroid
) {

    /**
     * Delete an existing Mega Chat message
     *
     * @param chatRoomId    MegaChatHandle that identifies the chat room
     * @param messageId     MegaChatHandle that identifies the message
     * @return              Completable
     */
    fun delete(chatRoomId: Long, messageId: Long): Completable =
        Completable.fromAction {
            val message = megaChatApi.getMessage(chatRoomId, messageId)
                ?: error("Message not found")

            when {
                message.type == TYPE_VOICE_CLIP -> {
                    message.megaNodeList?.get(0)?.let(::deleteVoiceClipFile)

                    megaChatApi.revokeAttachmentMessage(chatRoomId, messageId)
                        ?: error("Voice clip message too old to be deleted")
                }
                message.type == TYPE_NODE_ATTACHMENT ->
                    megaChatApi.revokeAttachmentMessage(chatRoomId, messageId)
                        ?: error("Attachment message too old to be deleted")
                message.status == STATUS_SENDING ->
                    megaChatApi.deleteMessage(chatRoomId, message.tempId)
                        ?: error("Sending Message could not be deleted")
                else ->
                    megaChatApi.deleteMessage(chatRoomId, messageId)
                        ?: error("Message too old to be deleted")
            }
        }

    /**
     * Check if chat message can be deleted. Return true if it's the case, false otherwise.
     *
     * @param chatRoomId    Chat Room Id required to obtain Chat Message
     * @param messageId     Chat Message Id to check
     * @return              Single with true if can be deleted, false otherwise.
     */
    fun check(chatRoomId: Long, messageId: Long): Single<Boolean> =
        Single.fromCallable {
            megaChatApi.getMessage(chatRoomId, messageId)?.isDeletable
                ?: error("Message not found")
        }

    /**
     * Delete Voice Clip node file
     *
     * @param node  Node file to be deleted
     */
    private fun deleteVoiceClipFile(node: MegaNode) {
        val localFile = CacheFolderManager.buildVoiceClipFile(context, node.name)
        if (FileUtil.isFileAvailable(localFile)) localFile.delete()
    }
}
