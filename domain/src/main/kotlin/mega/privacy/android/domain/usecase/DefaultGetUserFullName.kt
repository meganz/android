package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Implementation of [GetUserFullName]
 */
class DefaultGetUserFullName @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
) : GetUserFullName {

    override suspend fun invoke(forceRefresh: Boolean) =
        getName(forceRefresh) ?: getNameFromEmail()

    private suspend fun getName(forceRefresh: Boolean): String? {
        val firstName = contactsRepository.getCurrentUserFirstName(forceRefresh = forceRefresh)
        val lastName = contactsRepository.getCurrentUserLastName(forceRefresh = forceRefresh)

        return if (firstName.isBlank()) {
            lastName
        } else {
            "$firstName $lastName"
        }.takeUnless { it.isBlank() }
    }

    private suspend fun getNameFromEmail() = accountRepository.getAccountEmail()
        ?.split("[@._]".toRegex())?.get(0).takeUnless { it.isNullOrBlank() }
}