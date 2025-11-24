package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactLinkQueryResult
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import javax.inject.Inject

/**
 * Get Contact From Link Use Case
 *
 */
class ContactLinkQueryFromLinkUseCase @Inject constructor(
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase,
    private val contactLinkQueryUseCase: ContactLinkQueryUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(link: String): ContactLinkQueryResult? =
        getHandleFromContactLinkUseCase(link)
            .takeIf { it != -1L }
            ?.let { handle ->
                contactLinkQueryUseCase(handle)
            }
}