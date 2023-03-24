package mega.privacy.android.data.mapper.meeting

import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import nz.mega.sdk.MegaChatScheduledMeeting
import javax.inject.Inject

/**
 * Chat scheduled meeting mapper
 */
internal class ChatScheduledMeetingMapper @Inject constructor(
    private val chatScheduledMeetingChangesMapper: ChatScheduledMeetingChangesMapper,
    private val chatScheduledMeetingFlagsMapper: ChatScheduledMeetingFlagsMapper,
    private val chatScheduledMeetingRulesMapper: ChatScheduledMeetingRulesMapper,
) {
    operator fun invoke(megaChatScheduledMeeting: MegaChatScheduledMeeting): ChatScheduledMeeting =
        ChatScheduledMeeting(
            megaChatScheduledMeeting.chatId(),
            megaChatScheduledMeeting.schedId(),
            megaChatScheduledMeeting.parentSchedId(),
            megaChatScheduledMeeting.organizerUserId(),
            megaChatScheduledMeeting.timezone(),
            megaChatScheduledMeeting.startDateTime(),
            megaChatScheduledMeeting.endDateTime(),
            megaChatScheduledMeeting.title(),
            megaChatScheduledMeeting.description(),
            megaChatScheduledMeeting.attributes(),
            megaChatScheduledMeeting.overrides(),
            chatScheduledMeetingFlagsMapper(megaChatScheduledMeeting.flags()),
            chatScheduledMeetingRulesMapper(megaChatScheduledMeeting.rules()),
            chatScheduledMeetingChangesMapper(megaChatScheduledMeeting),
            megaChatScheduledMeeting.isCanceled()
        )

    private fun MegaChatScheduledMeeting.isCanceled(): Boolean =
        cancelled() > 0

}
