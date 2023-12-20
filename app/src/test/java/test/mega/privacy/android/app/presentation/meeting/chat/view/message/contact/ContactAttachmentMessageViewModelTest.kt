package test.mega.privacy.android.app.presentation.meeting.chat.view.message.contact

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.ContactAttachmentMessageViewModel
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactAttachmentMessageViewModelTest {
    private lateinit var underTest: ContactAttachmentMessageViewModel
    private val getContactFromEmailUseCase: GetContactFromEmailUseCase = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(getContactFromEmailUseCase)
    }

    @Test
    fun `test that loadContactInfo() returns correctly when load from cache`() = runTest {
        val contactItem = ContactItem(
            contactData = ContactData(
                fullName = "fullName",
                alias = "alias",
                avatarUri = null,
            ),
            status = UserChatStatus.Away,
            visibility = UserVisibility.Visible,
            handle = 1234567890L,
            email = "email",
            defaultAvatarColor = null,
            timestamp = 1234567890L,
            areCredentialsVerified = true,
        )
        whenever(getContactFromEmailUseCase.invoke("email", false)).thenReturn(contactItem)
        Truth.assertThat(underTest.loadContactInfo("email")).isEqualTo(contactItem)
    }

    @Test
    fun `test that loadContactInfo() returns correctly when load from sdk`() = runTest {
        val cacheContactItem = ContactItem(
            contactData = ContactData(
                fullName = "",
                alias = "alias",
                avatarUri = null,
            ),
            status = UserChatStatus.Away,
            visibility = UserVisibility.Visible,
            handle = 1234567890L,
            email = "email",
            defaultAvatarColor = null,
            timestamp = 1234567890L,
            areCredentialsVerified = true,
        )
        val newContactItem = ContactItem(
            contactData = ContactData(
                fullName = "fullName",
                alias = "alias",
                avatarUri = null,
            ),
            status = UserChatStatus.Away,
            visibility = UserVisibility.Visible,
            handle = 1234567890L,
            email = "email",
            defaultAvatarColor = null,
            timestamp = 1234567890L,
            areCredentialsVerified = true,
        )
        whenever(getContactFromEmailUseCase.invoke("email", false)).thenReturn(cacheContactItem)
        whenever(getContactFromEmailUseCase.invoke("email", true)).thenReturn(newContactItem)
        Truth.assertThat(underTest.loadContactInfo("email")).isEqualTo(newContactItem)
    }

    private fun initTestClass() {
        underTest = ContactAttachmentMessageViewModel(getContactFromEmailUseCase)
    }
}