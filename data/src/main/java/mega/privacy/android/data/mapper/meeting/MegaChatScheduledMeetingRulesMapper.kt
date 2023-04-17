package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import nz.mega.sdk.MegaChatScheduledRules
import javax.inject.Inject

/**
 * Mapper to convert [ChatScheduledRules] to [MegaChatScheduledRules]
 */
internal class MegaChatScheduledMeetingRulesMapper @Inject constructor(private val megaOccurrenceFrequencyTypeMapper: MegaOccurrenceFrequencyTypeMapper) {
    operator fun invoke(chatScheduledRules: ChatScheduledRules?): MegaChatScheduledRules? =
        chatScheduledRules?.let { rules ->
            return@let MegaChatScheduledRules.createInstance(megaOccurrenceFrequencyTypeMapper(rules.freq))
        }
}