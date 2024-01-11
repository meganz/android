package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import javax.inject.Inject

/**
 * Get Contact From Link Use Case
 *
 */
class GetContactFromLinkUseCase @Inject constructor(
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase,
    private val getContactLinkUseCase: GetContactLinkUseCase,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(link: String): ContactLink? =
        getHandleFromContactLinkUseCase(link)
            .takeIf { it != -1L }
            ?.let { handle ->
                getContactLinkUseCase(handle)
            }
}