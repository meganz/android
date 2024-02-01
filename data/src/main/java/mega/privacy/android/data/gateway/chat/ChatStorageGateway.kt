package mega.privacy.android.data.gateway.chat

import androidx.paging.PagingSource
import mega.privacy.android.data.database.entity.chat.ChatGeolocationEntity
import mega.privacy.android.data.database.entity.chat.ChatNodeEntity
import mega.privacy.android.data.database.entity.chat.GiphyEntity
import mega.privacy.android.data.database.entity.chat.MetaTypedMessageEntity
import mega.privacy.android.data.database.entity.chat.RichPreviewEntity
import mega.privacy.android.data.database.entity.chat.TypedMessageEntity

/**
 * Chat storage gateway
 */
interface ChatStorageGateway {
    /**
     * Get typed message request paging source
     *
     * @param chatId
     * @return
     */
    fun getTypedMessageRequestPagingSource(chatId: Long): PagingSource<Int, MetaTypedMessageEntity>

    /**
     * Store messages
     *
     * @param messages
     * @param richPreviews
     * @param giphys
     * @param geolocations
     * @param chatNodes
     */
    suspend fun storeMessages(
        messages: List<TypedMessageEntity>,
        richPreviews: List<RichPreviewEntity>,
        giphys: List<GiphyEntity>,
        geolocations: List<ChatGeolocationEntity>,
        chatNodes: List<ChatNodeEntity>,
    )

    /**
     * Clear chat messages
     *
     * @param chatId
     */
    suspend fun clearChatMessages(chatId: Long)

    /**
     * Get next message
     *
     * @param chatId
     * @param timestamp
     * @return
     */
    suspend fun getNextMessage(chatId: Long, timestamp: Long): TypedMessageEntity?

}