package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import nz.mega.sdk.MegaChatScheduledRules
import javax.inject.Inject

/**
 * Mapper to convert [ChatScheduledRules] to [MegaChatScheduledRules]
 *
 * @property megaOccurrenceFrequencyTypeMapper      [MegaOccurrenceFrequencyTypeMapper]
 * @property megaIntegerWeekDaysListMapper          [MegaIntegerWeekDaysListMapper]
 * @property megaIntegerListMapper                  [MegaIntegerListMapper]
 */
internal class MegaChatScheduledMeetingRulesMapper @Inject constructor(
    private val megaOccurrenceFrequencyTypeMapper: MegaOccurrenceFrequencyTypeMapper,
    private val megaIntegerWeekDaysListMapper: MegaIntegerWeekDaysListMapper,
    private val megaIntegerListMapper: MegaIntegerListMapper,
) {
    operator fun invoke(chatScheduledRules: ChatScheduledRules?): MegaChatScheduledRules? =
        chatScheduledRules?.let { rules ->
            if (chatScheduledRules.freq == OccurrenceFrequencyType.Invalid) return null

            return@let MegaChatScheduledRules.createInstance(
                megaOccurrenceFrequencyTypeMapper(rules.freq),
                rules.interval,
                rules.until,
                megaIntegerWeekDaysListMapper(rules.weekDayList),
                megaIntegerListMapper(rules.monthDayList)
            )
        }
}