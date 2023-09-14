package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.contacts.OnlineStatus
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case for monitoring updates on chat online statuses of current user.
 */
class MonitorMyChatOnlineStatusUseCase @Inject constructor(private val contactsRepository: ContactsRepository) {

    /**
     * Invoke.
     *
     * @return Flow of [OnlineStatus].
     */
    operator fun invoke(): Flow<OnlineStatus> =
        contactsRepository.monitorMyChatOnlineStatusUpdates()
}