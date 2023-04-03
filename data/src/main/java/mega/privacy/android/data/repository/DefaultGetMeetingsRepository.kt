package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.MeetingParticipantsResult
import mega.privacy.android.domain.entity.meeting.OccurrenceFrequencyType
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingResult
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.GetNextSchedMeetingOccurrenceUseCase
import javax.inject.Inject

/**
 * Default implementation of [GetMeetingsRepository]
 *
 * @property accountRepository
 * @property avatarRepository
 * @property getUserFirstName
 * @property chatParticipantsRepository
 * @property getScheduledMeetingByChat
 * @property getNextSchedMeetingOccurrence
 * @property getChatRoom
 * @property getChatCall
 * @property megaChatApiGateway
 */
internal class DefaultGetMeetingsRepository @Inject constructor(
    private val accountRepository: AccountRepository,
    private val avatarRepository: AvatarRepository,
    private val getUserFirstName: GetUserFirstName,
    private val chatParticipantsRepository: ChatParticipantsRepository,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getNextSchedMeetingOccurrence: GetNextSchedMeetingOccurrenceUseCase,
    private val getChatRoom: GetChatRoom,
    private val getChatCall: GetChatCall,
    private val megaChatApiGateway: MegaChatApiGateway,
) : GetMeetingsRepository {

    override suspend fun getMeetingParticipants(chatId: Long): MeetingParticipantsResult {
        val chatRoom = getChatRoom(chatId) ?: error("Chat room does not exist")
        val participants = chatParticipantsRepository.getChatParticipantsHandles(chatId)
        val myAccount = accountRepository.getUserAccount()
        val myHandle = myAccount.userId?.id ?: -1

        var firstUserChar: String? = null
        var firstUserAvatar: String? = null
        var firstUserColor: Int? = null
        var secondUserChar: String? = null
        var secondUserAvatar: String? = null
        var secondUserColor: Int? = null
        when {
            !chatRoom.isActive || participants.isEmpty() -> {
                firstUserChar = chatRoom.title
                firstUserColor = null
            }
            participants.size == 1 -> {
                firstUserChar = myAccount.fullName
                firstUserAvatar = avatarRepository.getMyAvatarFile()?.absolutePath
                firstUserColor = avatarRepository.getAvatarColor(myHandle)
                participants.getOrNull(0)?.let { userHandle ->
                    secondUserChar = getUserFirstCharacter(userHandle)
                    secondUserAvatar = getAvatarPath(userHandle)
                    secondUserColor = avatarRepository.getAvatarColor(userHandle)
                }
            }
            else -> {
                participants.firstOrNull()?.let { userHandle ->
                    if (userHandle == myHandle) {
                        firstUserChar = myAccount.fullName
                        firstUserAvatar = avatarRepository.getMyAvatarFile()?.absolutePath
                        firstUserColor = avatarRepository.getAvatarColor(myHandle)
                    } else {
                        firstUserChar = getUserFirstCharacter(userHandle)
                        firstUserAvatar = getAvatarPath(userHandle)
                        firstUserColor = avatarRepository.getAvatarColor(userHandle)
                    }
                }
                participants.getOrNull(1)?.let { userHandle ->
                    if (userHandle == myHandle) {
                        secondUserChar = myAccount.fullName
                        secondUserAvatar = avatarRepository.getMyAvatarFile()?.absolutePath
                        secondUserColor = avatarRepository.getAvatarColor(myHandle)
                    } else {
                        secondUserChar = getUserFirstCharacter(userHandle)
                        secondUserAvatar = getAvatarPath(userHandle)
                        secondUserColor = avatarRepository.getAvatarColor(userHandle)
                    }
                }
            }
        }

        return MeetingParticipantsResult(
            firstUserChar = firstUserChar,
            firstUserAvatar = firstUserAvatar,
            firstUserColor = firstUserColor,
            secondUserChar = secondUserChar,
            secondUserAvatar = secondUserAvatar,
            secondUserColor = secondUserColor,
        )
    }

    override suspend fun getMeetingScheduleData(chatId: Long): ScheduledMeetingResult? =
        getScheduledMeetingByChat(chatId)
            ?.firstOrNull { !it.isCanceled && it.parentSchedId == megaChatApiGateway.getChatInvalidHandle() }
            ?.let { schedMeeting ->
                val chatRoom = getChatRoom(chatId) ?: error("Chat room does not exist")
                val isPending = chatRoom.isActive && schedMeeting.isPending()
                val isRecurringDaily = schedMeeting.rules?.freq == OccurrenceFrequencyType.Daily
                val isRecurringWeekly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Weekly
                val isRecurringMonthly = schedMeeting.rules?.freq == OccurrenceFrequencyType.Monthly
                var startTimestamp = schedMeeting.startDateTime
                var endTimestamp = schedMeeting.endDateTime
                val scheduledMeetingStatus = getScheduledMeetingStatus(schedMeeting.chatId)

                if (isPending && schedMeeting.rules != null) {
                    runCatching { getNextSchedMeetingOccurrence(chatId) }.getOrNull()?.let {
                        startTimestamp = it.startDateTime
                        endTimestamp = it.endDateTime
                    }
                }

                ScheduledMeetingResult(
                    schedId = schedMeeting.schedId,
                    scheduledStartTimestamp = startTimestamp,
                    scheduledEndTimestamp = endTimestamp,
                    isRecurringDaily = isRecurringDaily,
                    isRecurringWeekly = isRecurringWeekly,
                    isRecurringMonthly = isRecurringMonthly,
                    isPending = isPending,
                    scheduledMeetingStatus = scheduledMeetingStatus
                )
            }

    override suspend fun getScheduledMeetingStatus(chatId: Long): ScheduledMeetingStatus =
        getChatCall(chatId)?.status?.let { status ->
            when (status) {
                ChatCallStatus.Connecting,
                ChatCallStatus.Joining,
                ChatCallStatus.InProgress,
                -> ScheduledMeetingStatus.Joined
                ChatCallStatus.UserNoPresent -> ScheduledMeetingStatus.NotJoined
                else -> ScheduledMeetingStatus.NotStarted
            }
        } ?: ScheduledMeetingStatus.NotStarted

    private suspend fun getUserFirstCharacter(userHandle: Long): String? =
        runCatching { getUserFirstName(userHandle, skipCache = false, shouldNotify = false) }
            .getOrNull()

    private suspend fun getAvatarPath(userHandle: Long): String? =
        runCatching { avatarRepository.getAvatarFile(userHandle) }
            .getOrNull()?.absolutePath
}
