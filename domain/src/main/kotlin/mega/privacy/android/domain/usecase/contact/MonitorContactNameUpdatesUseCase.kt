package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Monitor contact name updates use case.
 */
class MonitorContactNameUpdatesUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
) {
    /**
     * Invoke.
     *
     * @return Flow of contact name updates.
     */
    operator fun invoke(updateContactCache: Boolean = false) =
        contactsRepository.monitorContactUpdates()
            .filter { updates ->
                updates.changes.values.any { changes ->
                    changes.any(isObservedChange)
                }
            }.onEach {
                if (updateContactCache) {
                    contactsRepository.updateContactCache(it)
                }
            }

    private val isObservedChange = { change: UserChanges ->
        change == UserChanges.AuthenticationInformation ||
                change == UserChanges.Firstname ||
                change == UserChanges.Lastname ||
                change == UserChanges.Alias
    }
}