package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ApplyContactUpdatesUseCaseTest {
    private val repository = mock<ContactsRepository>()
    private val userId = UserId(123456L)
    private lateinit var userUpdate: UserUpdate
    private val mockContactData = ContactData(
        alias = "Iron Man",
        avatarUri = "https://avatar.uri.com",
        fullName = "Tony Stark",
    )
    private val contactItem = ContactItem(
        handle = 123456,
        email = "test@gmail.com",
        contactData = mockContactData,
        defaultAvatarColor = "red",
        visibility = UserVisibility.Visible,
        timestamp = 123456789,
        areCredentialsVerified = true,
        status = UserStatus.Online,
        lastSeen = 0,
    )
    private val underTest = ApplyContactUpdatesUseCase(repository)


    @Test
    fun `test that avatar is updated when avatar update is received`() = runTest {
        userUpdate = UserUpdate(changes = mapOf(userId to listOf(UserChanges.Avatar)))
        whenever(repository.deleteAvatar(any())).thenReturn(null)
        whenever(repository.getAvatarUri(any())).thenReturn("FileUri")
        val contact = underTest(contactItem, userUpdate)
        assertEquals(contact.email, contactItem.email)
        assertEquals("FileUri", contact.contactData.avatarUri)
    }

    @Test
    fun `test that full name is updated when first name update is received`() = runTest {
        userUpdate = UserUpdate(changes = mapOf(userId to listOf(UserChanges.Firstname)))
        whenever(repository.getUserFullName(any(), any())).thenReturn("Tony Stark New")
        val contact = underTest(contactItem, userUpdate)
        assertEquals(contact.email, contactItem.email)
        assertEquals("Tony Stark New", contact.contactData.fullName)
    }

    @Test
    fun `test that full name is updated when last name update is received`() = runTest {
        userUpdate = UserUpdate(changes = mapOf(userId to listOf(UserChanges.Lastname)))
        whenever(repository.getUserFullName(any(), any())).thenReturn("Tony Stark New")
        val contact = underTest(contactItem, userUpdate)
        assertEquals(contact.email, contactItem.email)
        assertEquals("Tony Stark New", contact.contactData.fullName)
    }

    @Test
    fun `test that nick name is updated when nick name update is received`() = runTest {
        userUpdate = UserUpdate(changes = mapOf(userId to listOf(UserChanges.Alias)))
        whenever(repository.getUserAlias(any())).thenReturn("Nick Name")
        val contact = underTest(contactItem, userUpdate)
        assertEquals(contact.email, contactItem.email)
        assertEquals("Nick Name", contact.contactData.alias)
    }

    @Test
    fun `test that email is updated when email update is received`() = runTest {
        userUpdate = UserUpdate(changes = mapOf(userId to listOf(UserChanges.Email)))
        whenever(repository.getUserEmail(any(), any())).thenReturn("updated@gmail.com")
        val contact = underTest(contactItem, userUpdate)
        assertEquals("updated@gmail.com", contact.email)
    }
}