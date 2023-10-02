package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.chat.ChatPreview
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatRequest
import javax.inject.Inject

/**
 * Chat preview mapper
 *
 * @property chatRequestMapper
 */
class ChatPreviewMapper @Inject constructor(
    private val chatRequestMapper: ChatRequestMapper,
) {
    /**
     * Invoke
     *
     * @param request
     * @param errorCode
     */
    operator fun invoke(request: MegaChatRequest, errorCode: Int) = ChatPreview(
        chatRequestMapper(request),
        errorCode == MegaChatError.ERROR_EXIST
    )
}