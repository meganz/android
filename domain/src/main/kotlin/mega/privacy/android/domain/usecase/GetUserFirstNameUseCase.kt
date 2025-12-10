package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import javax.inject.Inject

/**
 * Get current user's full name
 */
class GetUserFirstNameUseCase @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val accountRepository: AccountRepository,
) {

    /**
     * Invoke
     * @param forceRefresh true force to load from sdk otherwise use database cache
     * @return full name
     */
    suspend operator fun invoke(forceRefresh: Boolean) =
        getFirstName(forceRefresh) ?: getNameFromEmail()


    private suspend fun getFirstName(forceRefresh: Boolean) =
        runCatching {
            contactsRepository.getCurrentUserFirstName(forceRefresh = forceRefresh).trim()
                .takeUnless { it.isBlank() }
        }.getOrNull()

    private suspend fun getNameFromEmail() = accountRepository.getAccountEmail()
        ?.split("[@._]".toRegex())?.get(0).takeUnless { it.isNullOrBlank() }
}
