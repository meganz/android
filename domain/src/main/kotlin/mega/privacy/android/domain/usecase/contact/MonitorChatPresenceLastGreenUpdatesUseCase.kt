package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.user.UserLastGreen
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Use case for monitoring updates on last green.
 */
class MonitorChatPresenceLastGreenUpdatesUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {

    /**
     * Invoke.
     *
     * @return Flow of [UserLastGreen].
     */
    operator fun invoke(): Flow<UserLastGreen> =
        contactsRepository.monitorChatPresenceLastGreenUpdates()
}