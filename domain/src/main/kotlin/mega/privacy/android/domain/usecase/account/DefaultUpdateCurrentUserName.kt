package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

internal class DefaultUpdateCurrentUserName @Inject constructor(
    private val repository: ContactsRepository
) : UpdateCurrentUserName {
    override suspend fun invoke(
        oldFirstName: String,
        oldLastName: String,
        newFirstName: String,
        newLastName: String,
    ) {
        coroutineScope {
            val changes = mutableListOf<Deferred<String>>()
            if (oldFirstName != newFirstName) {
                changes.add(
                    async { repository.updateCurrentUserFirstName(newFirstName) }
                )
            }
            if (oldLastName != newLastName) {
                changes.add(
                    async { repository.updateCurrentUserLastName(newLastName) }
                )
            }
            changes.awaitAll()
        }
    }
}