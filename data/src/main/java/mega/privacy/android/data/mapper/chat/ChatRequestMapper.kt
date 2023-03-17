package mega.privacy.android.data.mapper.chat

import mega.privacy.android.domain.entity.ChatRequest
import nz.mega.sdk.MegaChatRequest

/**
 * Mapper to convert data into [ChatRequest].
 */
fun interface ChatRequestMapper {

    /**
     * Invoke.
     *
     * @param megaChatRequest [MegaChatRequest]
     * @return [ChatRequest]
     */
    operator fun invoke(megaChatRequest: MegaChatRequest): ChatRequest
}