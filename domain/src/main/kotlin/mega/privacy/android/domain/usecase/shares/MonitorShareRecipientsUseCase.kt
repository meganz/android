package mega.privacy.android.domain.usecase.shares

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

class MonitorShareRecipientsUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val contactsRepository: ContactsRepository,
    private val avatarRepository: AvatarRepository,
) {

    operator fun invoke(nodeId: NodeId): Flow<List<ShareRecipient>> =
        flow {
            val shareDataList = nodeRepository.getNodeOutgoingShares(nodeId)
            if (shareDataList.isEmpty()) {
                emit(emptyList())
            } else {
                emitAll(
                    getContactListFlow(shareDataList)
                )
            }
            awaitCancellation()
        }

    private fun getContactListFlow(shareDataList: List<ShareData>): Flow<List<ShareRecipient>> =
        combine(
            shareDataList.mapNotNull { shareData ->
                val email = shareData.user ?: return@mapNotNull null
                getSingleContactFlow(email, shareData)
            }
        ) { it.toList() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getSingleContactFlow(
        email: String,
        data: ShareData,
    ): Flow<ShareRecipient> =
        contactsRepository.monitorContactByEmail(email)
            .flatMapLatest<Contact?, ShareRecipient> { contact ->
                contact?.let { contact: Contact ->
                    val areCredentialsVerified = contactsRepository.areCredentialsVerified(email)
                    val defaultAvatarColor = avatarRepository.getAvatarColor(contact.userId)
                    contactsRepository.monitorOnlineStatusByHandle(contact.userId)
                        .map { onlineStatus ->
                            ShareRecipient.Contact(
                                handle = contact.userId,
                                email = email,
                                contactData = ContactData(
                                    fullName = contact.fullName,
                                    alias = contact.nickname,
                                    avatarUri = contactsRepository.getAvatarUri(email),
                                    userVisibility = if (contact.isVisible) UserVisibility.Visible else UserVisibility.Hidden,
                                ),
                                isVerified = areCredentialsVerified,
                                permission = data.access,
                                isPending = data.isPending,
                                status = onlineStatus,
                                defaultAvatarColor = defaultAvatarColor,
                            )
                        }
                } ?: flow {
                    emit(
                        ShareRecipient.NonContact(
                            email = email,
                            permission = data.access,
                            isPending = data.isPending,
                        )
                    )
                    awaitCancellation()
                }
            }
}
