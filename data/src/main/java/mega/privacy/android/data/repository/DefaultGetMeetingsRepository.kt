package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
import javax.inject.Inject

/**
 * Default implementation of [GetMeetingsRepository]
 *
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property megaApiGateway                     [MegaApiGateway]
 * @property ioDispatcher                       [CoroutineDispatcher]
 */
internal class DefaultGetMeetingsRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
    private val avatarRepository: AvatarRepository,
    private val chatParticipantsRepository: ChatParticipantsRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetMeetingsRepository {

    override suspend fun updateMeetingFields(items: MutableList<MeetingRoomItem>) {
        val iterator = items.listIterator()
        while (iterator.hasNext()) {
            val meeting = iterator.next()
            val participants = chatParticipantsRepository.getChatParticipantsHandles(meeting.chatId)
            val myAccount = accountRepository.getUserAccount()
            val myHandle = myAccount.userId?.id ?: -1

            var firstUserChar: Char? = null
            var firstUserAvatar: String? = null
            var firstUserColor: Int? = null
            var lastUserChar: Char? = null
            var lastUserAvatar: String? = null
            var lastUserColor: Int? = null
            when (participants.size) {
                0 -> {
                    firstUserChar = myAccount.fullName?.firstOrNull()
                    firstUserAvatar = avatarRepository.getMyAvatarFile()?.absolutePath
                    firstUserColor = avatarRepository.getAvatarColor(myHandle)
                }
                1 -> {
                    firstUserChar = myAccount.fullName?.firstOrNull()
                    firstUserAvatar = avatarRepository.getMyAvatarFile()?.absolutePath
                    firstUserColor = avatarRepository.getAvatarColor(myHandle)
                    participants.lastOrNull()?.let { userHandle ->
                        lastUserChar = getUserFirstCharacter(userHandle)
                        lastUserAvatar = getAvatarPath(userHandle)
                        lastUserColor = avatarRepository.getAvatarColor(userHandle)
                    }
                }
                else -> {
                    participants.firstOrNull()?.let { userHandle ->
                        if (userHandle == myHandle) {
                            firstUserChar = myAccount.fullName?.firstOrNull()
                            firstUserAvatar = avatarRepository.getMyAvatarFile()?.absolutePath
                            firstUserColor = avatarRepository.getAvatarColor(myHandle)
                        } else {
                            firstUserChar = getUserFirstCharacter(userHandle)
                            firstUserAvatar = getAvatarPath(userHandle)
                            firstUserColor = avatarRepository.getAvatarColor(userHandle)
                        }
                    }
                    participants.lastOrNull()?.let { userHandle ->
                        if (userHandle == myHandle) {
                            lastUserChar = myAccount.fullName?.firstOrNull()
                            lastUserAvatar = avatarRepository.getMyAvatarFile()?.absolutePath
                            lastUserColor = avatarRepository.getAvatarColor(myHandle)
                        } else {
                            lastUserChar = getUserFirstCharacter(userHandle)
                            lastUserAvatar = getAvatarPath(userHandle)
                            lastUserColor = avatarRepository.getAvatarColor(userHandle)
                        }
                    }
                }
            }

            iterator.set(meeting.copy(
                firstUserChar = firstUserChar,
                firstUserAvatar = firstUserAvatar,
                firstUserColor = firstUserColor,
                lastUserChar = lastUserChar,
                lastUserAvatar = lastUserAvatar,
                lastUserColor = lastUserColor,
            ))
        }
    }

    private suspend fun getUserFirstCharacter(userHandle: Long): Char? =
        runCatching { contactsRepository.getUserFirstName(userHandle) }
            .fold(
                onSuccess = { name -> name.first() },
                onFailure = { null }
            )

    private suspend fun getAvatarPath(userHandle: Long): String? =
        runCatching { avatarRepository.getAvatarFile(userHandle) }
            .fold(
                onSuccess = { file -> file?.absolutePath },
                onFailure = { null }
            )
}
