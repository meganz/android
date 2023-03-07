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
                                lastUserChar = updatedItem.lastUserChar,
                                lastUserAvatar = updatedItem.lastUserAvatar,
                                lastUserColor = updatedItem.lastUserColor,
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

        var firstUserChar: Char? = null
        var firstUserAvatar: String? = null
        var firstUserColor: Int? = null
        var lastUserChar: Char? = null
        var lastUserAvatar: String? = null
        var lastUserColor: Int? = null
        when {
            !item.isActive || participants.isEmpty() -> {
                firstUserChar = item.title.firstOrNull()
                firstUserColor = null
            }
            participants.size == 1 -> {
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

        return item.copy(
            firstUserChar = firstUserChar,
            firstUserAvatar = firstUserAvatar,
            firstUserColor = firstUserColor,
            lastUserChar = lastUserChar,
            lastUserAvatar = lastUserAvatar,
            lastUserColor = lastUserColor,
        )
    }

    private suspend fun getUserFirstCharacter(userHandle: Long): Char? =
        runCatching { getUserFirstName(userHandle, skipCache = false, shouldNotify = false) }
            .getOrNull()?.firstOrNull()

    private suspend fun getAvatarPath(userHandle: Long): String? =
        runCatching { avatarRepository.getAvatarFile(userHandle) }
            .getOrNull()?.absolutePath
}
