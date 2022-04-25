package mega.privacy.android.app.usecase.chat

import io.reactivex.rxjava3.core.Single
import mega.privacy.android.app.utils.ChatUtil
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatListItem
import javax.inject.Inject

/**
 * Use case to search chat list items
 *
 * @property megaChatApi    Needed to retrieve chat list items
 */
class SearchChatsUseCase @Inject constructor(
    private val megaChatApi: MegaChatApiAndroid
) {

    /**
     * Search chat list items based on a query string
     *
     * @param query         Query string to search
     * @param archivedChats Flag to include archived chats or not
     * @return              Single with a List of MegaChatListItem
     */
    fun search(query: String, archivedChats: Boolean): Single<List<MegaChatListItem>> =
        Single.fromCallable {
            val items = mutableListOf<MegaChatListItem>().apply {
                addAll(megaChatApi.chatListItems)
                if (archivedChats) addAll(megaChatApi.archivedChatListItems)
            }

            items
                .filter { item ->
                    val chatDateTitle = ChatUtil.getTitleChat(megaChatApi.getChatRoom(item.chatId))
                    val userAlias = megaChatApi.getUserAliasFromCache(item.peerHandle)
                    val userEmail = megaChatApi.getUserEmailFromCache(item.peerHandle)
                    val userFullName = megaChatApi.getUserFullnameFromCache(item.peerHandle)

                    item.title.contains(query, true)
                            || chatDateTitle.contains(query, true)
                            || item.lastMessage?.contains(query, true) == true
                            || userAlias?.contains(query, true) == true
                            || userEmail?.contains(query, true) == true
                            || userFullName?.contains(query, true) == true
                }
                .sortedByDescending { it.lastTimestamp }
        }
}
