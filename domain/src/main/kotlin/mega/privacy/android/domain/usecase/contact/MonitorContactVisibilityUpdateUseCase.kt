package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 *  Monitor contact visibility update use case, send out a flow of [UserId] whose visibility has changed.
 */
class MonitorContactVisibilityUpdateUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     *
     * @return Flow of [UserId] whose visibility has changed.
     */
    operator fun invoke(): Flow<UserId> = flow {
        accountRepository.monitorUserUpdates()
            .map { userUpdate ->
                userUpdate.changes
                    .map { (userId, userChanges) ->
                        userId.id to userChanges.filterIsInstance<UserChanges.Visibility>()
                            .firstOrNull()?.userVisibility
                    }.filter { it.second != null }.toMap()
            }.scan(getCurrentVisibilities()) { currentVisibilities, newChanges ->
                val changedUsers = getChangedUsers(currentVisibilities, newChanges)
                if (changedUsers.isNotEmpty()) {
                    emitAll(changedUsers.asFlow())
                    getCurrentVisibilities()
                } else {
                    currentVisibilities
                }
            }.collect {}
        awaitCancellation()
    }

    private suspend fun getCurrentVisibilities() = contactsRepository.getVisibleContacts()
        .associate { contact -> contact.handle to contact.visibility }

    /**
     * Compare the visibility of current contacts with the new visibility changes,
     * and return the list of [UserId] whose visibility has changed.
     *
     * @param currentVisibilities
     * @param newChanges
     * @return List of [UserId] whose visibility has changed.
     */
    private fun getChangedUsers(
        currentVisibilities: Map<Long, UserVisibility>,
        newChanges: Map<Long, UserVisibility?>,
    ): List<UserId> = newChanges.filter { (userId, newVisibility) ->
        newVisibility != null && newVisibility != currentVisibilities[userId]
    }.map { (userId, _) -> UserId(userId) }
}

