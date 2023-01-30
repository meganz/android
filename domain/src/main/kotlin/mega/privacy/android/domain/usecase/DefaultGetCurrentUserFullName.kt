package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Implementation of [GetCurrentUserFullName]
 */
class DefaultGetCurrentUserFullName @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
) : GetCurrentUserFullName {

    override suspend fun invoke(
        forceRefresh: Boolean,
        defaultFirstName: String,
        defaultLastName: String
    ): String =
        withContext(ioDispatcher) {
            val firstName = contactsRepository.getCurrentUserFirstName(forceRefresh = forceRefresh)
            val lastName = contactsRepository.getCurrentUserLastName(forceRefresh = forceRefresh)

            var fullName: String = if (firstName.isBlank()) {
                lastName
            } else {
                "$firstName $lastName"
            }

            if (fullName.isBlank()) {
                accountRepository.accountEmail
                    ?.takeIf { it.isNotBlank() }
                    ?.split("[@._]".toRegex())?.toTypedArray()
                    ?.get(0)
                    ?.let { fullName = it }
            }

            if (fullName.isBlank()) {
                fullName = "$defaultFirstName $defaultLastName"
            }

            return@withContext fullName
        }
}