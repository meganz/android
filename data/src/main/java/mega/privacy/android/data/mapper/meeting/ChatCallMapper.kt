package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatCall
import nz.mega.sdk.MegaChatCall

/**
 * Call mapper
 */
internal fun interface ChatCallMapper {
    /**
     * Invoke
     *
     * @param megaChatCall
     * @return [ChatCall]
     */
    operator fun invoke(megaChatCall: MegaChatCall): ChatCall
}