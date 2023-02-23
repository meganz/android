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

    private val contactItem = ContactItem(
        handle = 2L,
        email = "email",
        contactData = ContactData("name", "alias", "avatar"),
        defaultAvatarColor = "color",
        visibility = UserVisibility.Unknown,
        timestamp = 0L,
        areCredentialsVerified = true,
        status = UserStatus.Online,
        lastSeen = null
    )
    private val nodeRepository = mock<NodeRepository> {
        onBlocking { getOwnerIdFromInShare(nodeId, false) }.thenReturn(userId)
    }
    private val getContactItem = mock<GetContactItem> {
        onBlocking { invoke(userId) }.thenReturn(contactItem)
    }


    @Before
    fun init() {
        underTest = DefaultGetContactItemFromInShareFolder(
            getContactItem,
            nodeRepository,
        )
    }

    @Test
    fun `test DefaultGetContactItemFromInShareFolder returns correct info from getContactItem userCase`() =
        runTest {
            val actual = underTest(node)
            Truth.assertThat(actual).isSameInstanceAs(contactItem)
        }
}