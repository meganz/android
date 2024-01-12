package test.mega.privacy.android.app.presentation.meeting.chat.view.message.link

import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ChatLinkMessageViewModel
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ContactLinkContent
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.usecase.contact.GetContactFromLinkUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatLinkMessageViewModelTest {
    private lateinit var underTest: ChatLinkMessageViewModel
    private val getContactFromLinkUseCase: GetContactFromLinkUseCase = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        underTest = ChatLinkMessageViewModel(getContactFromLinkUseCase)
    }

    @Test
    fun `test that load contact info return correctly`() = runTest {
        val link = "link"
        val contactLink = mock<ContactLink>()
        whenever(getContactFromLinkUseCase(link)).thenReturn(contactLink)
        underTest.loadContactInfo(link)
        // make sure it is called only once because the other call we load from cache
        verify(getContactFromLinkUseCase).invoke(link)
        Truth.assertThat(underTest.loadContactInfo(link))
            .isEqualTo(ContactLinkContent(contactLink, link))
    }
}