package mega.privacy.android.app.presentation.imagepreview.fetcher

import android.os.Bundle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.MonitorChatImageNodesUseCase
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatImageNodeFetcher @Inject constructor(
    private val monitorChatImageNodesUseCase: MonitorChatImageNodesUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ImageNodeFetcher {
    override fun monitorImageNodes(bundle: Bundle): Flow<List<ImageNode>> {
        return monitorChatImageNodesUseCase(
            chatRoomId = bundle.getLong(CHAT_ROOM_ID),
            messageIds = bundle.getLongArray(MESSAGE_IDS)?.map { it }.orEmpty(),
        ).mapLatest { imageNodes ->
            imageNodes.sortedWith(compareByDescending<ImageNode> { it.modificationTime }.thenByDescending { it.id.longValue })
        }.flowOn(defaultDispatcher)
    }

    internal companion object {
        const val CHAT_ROOM_ID: String = "chatRoomId"
        const val MESSAGE_IDS: String = "messageIds"
    }
}
