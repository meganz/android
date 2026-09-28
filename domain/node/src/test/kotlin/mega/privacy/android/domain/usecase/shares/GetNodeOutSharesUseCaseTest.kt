package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetNodeOutSharesUseCaseTest {

    private val nodeRepository = mock<NodeRepository>()
    private val contactsRepository = mock<ContactsRepository>()

    private val underTest = GetNodeOutSharesUseCase(nodeRepository, contactsRepository)

    private val nodeHandle = -1L
    private val nodeId = NodeId(nodeHandle)

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns an empty list then an empty list is returned`() =
        runTest {
            whenever(nodeRepository.getNodeOutgoingShares(nodeId)).thenReturn(emptyList())
            val actual = underTest(nodeId)
            Truth.assertThat(actual).isEmpty()
        }

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns some nodes then contact is fetched for each node with skip cache set to false`() =
        runTest {
            val emails = List(5) { "email$it" }
            val mocks = emails.map { email ->
                mock<ShareData> {
                    on { it.user }.thenReturn(email)
                }
            }
            whenever(nodeRepository.getNodeOutgoingShares(nodeId)).thenReturn(mocks)
            underTest(nodeId)
            emails.forEach {
                verify(contactsRepository).getContactItemFromUserEmail(it, false)
            }
        }

    @Test
    fun `test that when nodeRepository's getNodeOutgoingShares returns some nodes then correct data is returned`() =
        runTest {
            val sharedDataList = List(5) { index ->
                mock<ShareData> {
                    on { it.user }.thenReturn("email$index")
                    on { it.access }.thenReturn(AccessPermission.values()[index.mod(AccessPermission.values().size)])
                }
            }
            val sharedDataToContactItem = sharedDataList.associateWith {
                mock<ContactItem>()
            }
            whenever(nodeRepository.getNodeOutgoingShares(nodeId)).thenReturn(sharedDataList)
            sharedDataToContactItem.forEach { (sharedData, contactItem) ->
                whenever(
                    contactsRepository.getContactItemFromUserEmail(sharedData.user ?: "", false)
                ).thenReturn(contactItem)
            }
            val expected = sharedDataToContactItem.map { (sharedData, contactItem) ->
                ContactPermission(
                    contactItem = contactItem,
                    accessPermission = sharedData.access
                )
            }
            val actual = underTest(nodeId)
            Truth.assertThat(actual).containsExactlyElementsIn(expected)
        }
}