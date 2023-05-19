package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get current user's full name
 */
class GetUserFullNameUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     * @param forceRefresh true force to load from sdk otherwise use database cache
     * @return full name
     */
    suspend operator fun invoke(forceRefresh: Boolean) =
        getName(forceRefresh) ?: getNameFromEmail()

    private suspend fun getName(forceRefresh: Boolean) =
        "${getFirstName(forceRefresh)} ${getLastName(forceRefresh)}".trim()
            .takeUnless { it.isBlank() }

    private suspend fun getLastName(forceRefresh: Boolean) =
        runCatching { contactsRepository.getCurrentUserLastName(forceRefresh = forceRefresh) }
            .getOrDefault("")

    private suspend fun getFirstName(forceRefresh: Boolean) =
        runCatching { contactsRepository.getCurrentUserFirstName(forceRefresh = forceRefresh) }
            .getOrDefault("")

    private suspend fun getNameFromEmail() = accountRepository.getAccountEmail()
        ?.split("[@._]".toRegex())?.get(0).takeUnless { it.isNullOrBlank() }
}