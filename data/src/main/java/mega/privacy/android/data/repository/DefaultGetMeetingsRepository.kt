package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import mega.privacy.android.domain.repository.GetMeetingsRepository
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import javax.inject.Inject

/**
 * Default implementation of [GetMeetingsRepository]
 *
 * @property accountRepository
 * @property avatarRepository
 * @property getUserFirstName
 * @property chatParticipantsRepository
 */
internal class DefaultGetMeetingsRepository @Inject constructor(
    private val accountRepository: AccountRepository,
    private val avatarRepository: AvatarRepository,
    private val getUserFirstName: GetUserFirstName,
    private val chatParticipantsRepository: ChatParticipantsRepository,
) : GetMeetingsRepository {

    override suspend fun getUpdatedMeetingItems(
        items: MutableList<MeetingRoomItem>,
        mutex: Mutex,
    ): Flow<MutableList<MeetingRoomItem>> =
        flow {
            items.toList().forEach { item ->
                getUpdatedMeetingItem(item).let { updatedItem ->
                    mutex.withLock {
                        val newIndex = items.indexOfFirst { updatedItem.chatId == it.chatId }
                        if (newIndex != -1) {
                            val newUpdatedItem = items[newIndex].copy(
                                firstUserChar = updatedItem.firstUserChar,
                                firstUserAvatar = updatedItem.firstUserAvatar,
                                firstUserColor = updatedItem.firstUserColor,
                                secondUserChar = updatedItem.secondUserChar,
                                secondUserAvatar = updatedItem.secondUserAvatar,
                                secondUserColor = updatedItem.secondUserColor,
                            )
                            items[newIndex] = newUpdatedItem
                            emit(items)
                        }
                    }
                }
            }
        }

    override suspend fun getUpdatedMeetingItem(item: MeetingRoomItem): MeetingRoomItem {
        val participants = chatParticipantsRepository.getChatParticipantsHandles(item.chatId)
        val myAccount = accountRepository.getUserAccount()
        val myHandle = myAccount.userId?.id ?: -1

        var firstUserChar: String? = null
        var firstUserAvatar: String? = null
        var firstUserColor: Int? = null
        var secondUserChar: String? = null
        var secondUserAvatar: String? = null
        var secondUserColor: Int? = null
        when {
            !item.isActive || participants.isEmpty() -> {
                firstUserChar = item.title
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

        return item.copy(
            firstUserChar = firstUserChar,
            firstUserAvatar = firstUserAvatar,
            firstUserColor = firstUserColor,
            secondUserChar = secondUserChar,
            secondUserAvatar = secondUserAvatar,
            secondUserColor = secondUserColor,
        )
    }

    private suspend fun getUserFirstCharacter(userHandle: Long): String? =
        runCatching { getUserFirstName(userHandle, skipCache = false, shouldNotify = false) }
            .getOrNull()

    private suspend fun getAvatarPath(userHandle: Long): String? =
        runCatching { avatarRepository.getAvatarFile(userHandle) }
            .getOrNull()?.absolutePath
}
