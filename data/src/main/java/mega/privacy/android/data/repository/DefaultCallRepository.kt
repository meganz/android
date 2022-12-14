package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.CameraGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.usecase.StartChatCall
import timber.log.Timber
import javax.inject.Inject

internal class DefaultCallRepository @Inject constructor(
    private val chatApiGateway: MegaChatApiGateway,
    private val cameraGateway: CameraGateway,
    private val startChatCall: StartChatCall,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CallRepository {

    override suspend fun startCall(
        chatId: Long,
        video: Boolean,
        audio: Boolean,
    ): ChatCall? {
        cameraGateway.setFrontCamera()

        withContext(ioDispatcher) {
            runCatching {
                startChatCall(chatId, enabledVideo = video, enabledAudio = audio)
            }.onFailure { exception ->
                Timber.e(exception)
                return@withContext null
            }.onSuccess { resultStartCall ->
                resultStartCall.chatHandle?.let { resultChatId ->
                    return@withContext chatApiGateway.getChatCall(resultChatId)
                }
            }
        }

        return null
    }
}