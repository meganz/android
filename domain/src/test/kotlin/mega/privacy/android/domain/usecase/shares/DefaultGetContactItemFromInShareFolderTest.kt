package mega.privacy.android.domain.usecase.shares

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.contact.GetContactItem
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetContactItemFromInShareFolderTest {
    private lateinit var underTest: GetContactItemFromInShareFolder

    private val userId = UserId(1L)
    private val nodeId = NodeId(1342L)
    private val node = mock<TypedFolderNode> {
        on { id }.thenReturn(nodeId)
    }

    private val contactItem = mock<ContactItem>()
    private val cachedContactItem = mock<ContactItem>()
    private val nodeRepository = mock<NodeRepository> {
        onBlocking { getOwnerIdFromInShare(nodeId, false) }.thenReturn(userId)
    }
    private val getContactItem = mock<GetContactItem> {
        onBlocking { invoke(userId, true) }.thenReturn(contactItem)
        onBlocking { invoke(userId, false) }.thenReturn(cachedContactItem)
    }


    @Before
    fun init() {
        underTest = DefaultGetContactItemFromInShareFolder(
            getContactItem,
            nodeRepository,
        )
    }

    @Test
    fun `test DefaultGetContactItemFromInShareFolder returns fetched info from getContactItem use case if skip cache is true`() =
        runTest {
            val actual = underTest(node,true)
            Truth.assertThat(actual).isSameInstanceAs(contactItem)
        }

    @Test
    fun `test DefaultGetContactItemFromInShareFolder returns cached info from getContactItem use case if skip cache is false`() =
        runTest {
            val actual = underTest(node,false)
            Truth.assertThat(actual).isSameInstanceAs(cachedContactItem)
        }
}