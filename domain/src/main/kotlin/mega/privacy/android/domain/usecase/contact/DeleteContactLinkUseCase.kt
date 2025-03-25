package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * Use Case to delete a contact link
 */
class DeleteContactLinkUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    /**
     * Invocation function
     *
     * @param handle The contact link's handle to be deleted
     * If the parameter is INVALID_HANDLE, the active contact link is deleted
     */
    suspend operator fun invoke(handle: Long) = accountRepository.deleteContactLink(handle)
}