package mega.privacy.android.feature.contact.info

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromEmailUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.feature.contact.info.model.ContactInfoUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class ContactInfoViewModelTest {

    private lateinit var underTest: ContactInfoViewModel

    private val getContactFromEmailUseCase = mock<GetContactFromEmailUseCase>()
    private val getContactFromChatUseCase = mock<GetContactFromChatUseCase>()
    private val getChatRoomByUserUseCase = mock<GetChatRoomByUserUseCase>()
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()

    private val chatRoom = mock<ChatRoom> {
        on { this.chatId } doReturn CHAT_ID
    }

    @BeforeEach
    fun setUp() {
        whenever(isConnectedToInternetUseCase()).thenReturn(true)
        underTest = createViewModel(email = EMAIL, chatId = null)
    }

    @AfterEach
    fun tearDown() {
        reset(
            getContactFromEmailUseCase,
            getContactFromChatUseCase,
            getChatRoomByUserUseCase,
            isConnectedToInternetUseCase,
        )
    }

    private fun createViewModel(email: String?, chatId: Long?) = ContactInfoViewModel(
        email = email,
        chatId = chatId,
        getContactFromEmailUseCase = getContactFromEmailUseCase,
        getContactFromChatUseCase = getContactFromChatUseCase,
        getChatRoomByUserUseCase = getChatRoomByUserUseCase,
        isConnectedToInternetUseCase = isConnectedToInternetUseCase,
    )

    @Test
    fun `test that initial state is Loading`() = runTest {
        assertThat(underTest.uiState.value)
            .isEqualTo(ContactInfoUiState.Loading(closeEvent = consumed))
    }

    @Test
    fun `test that state is Loaded with contact data when initialised with email`() = runTest {
        whenever(getContactFromEmailUseCase(EMAIL, true)).thenReturn(createContactItem())
        whenever(getChatRoomByUserUseCase(USER_HANDLE)).thenReturn(chatRoom)

        underTest.uiState.test {
            val actual = awaitLoadedState()
            assertThat(actual.displayName).isEqualTo(ALIAS)
            assertThat(actual.email).isEqualTo(EMAIL)
            assertThat(actual.userHandle).isEqualTo(USER_HANDLE)
            assertThat(actual.chatRoomId).isEqualTo(CHAT_ID)
            assertThat(actual.isFromContacts).isTrue()
        }
    }

    @Test
    fun `test that Loaded has null chatRoomId when no chat room exists for the contact`() =
        runTest {
            whenever(getContactFromEmailUseCase(EMAIL, true)).thenReturn(createContactItem())
            whenever(getChatRoomByUserUseCase(USER_HANDLE)).thenReturn(null)

            underTest.uiState.test {
                assertThat(awaitLoadedState().chatRoomId).isNull()
            }
        }

    @Test
    fun `test that state is Loaded when initialised with chat id`() = runTest {
        underTest = createViewModel(email = null, chatId = CHAT_ID)
        whenever(getContactFromChatUseCase(CHAT_ID, true)).thenReturn(createContactItem())

        underTest.uiState.test {
            val actual = awaitLoadedState()
            assertThat(actual.displayName).isEqualTo(ALIAS)
            assertThat(actual.email).isEqualTo(EMAIL)
            assertThat(actual.chatRoomId).isEqualTo(CHAT_ID)
            assertThat(actual.isFromContacts).isFalse()
        }
    }

    @Test
    fun `test that displayName falls back to full name when alias is null`() = runTest {
        whenever(getContactFromEmailUseCase(EMAIL, true))
            .thenReturn(createContactItem(alias = null))

        underTest.uiState.test {
            assertThat(awaitLoadedState().displayName).isEqualTo(FULL_NAME)
        }
    }

    @Test
    fun `test that displayName falls back to email when alias and full name are null`() = runTest {
        whenever(getContactFromEmailUseCase(EMAIL, true))
            .thenReturn(createContactItem(alias = null, fullName = null))

        underTest.uiState.test {
            assertThat(awaitLoadedState().displayName).isEqualTo(EMAIL)
        }
    }

    @Test
    fun `test that close event is triggered when contact resolution returns null`() = runTest {
        whenever(getContactFromEmailUseCase(EMAIL, true)).thenReturn(null)

        underTest.uiState.test {
            var item = awaitItem()
            while (item.closeEvent != triggered) {
                item = awaitItem()
            }
            assertThat(item.closeEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that close event is triggered when contact resolution throws`() = runTest {
        whenever(getContactFromEmailUseCase(EMAIL, true))
            .thenThrow(RuntimeException("resolution failed"))

        underTest.uiState.test {
            var item = awaitItem()
            while (item.closeEvent != triggered) {
                item = awaitItem()
            }
            assertThat(item.closeEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that onCloseEventConsumed resets the close event`() = runTest {
        whenever(getContactFromEmailUseCase(EMAIL, true)).thenReturn(null)

        underTest.uiState.test {
            var item = awaitItem()
            while (item.closeEvent != triggered) {
                item = awaitItem()
            }
            underTest.onCloseEventConsumed()
            while (item.closeEvent != consumed) {
                item = awaitItem()
            }
            assertThat(item.closeEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that contact is resolved from cache when device is offline`() = runTest {
        whenever(isConnectedToInternetUseCase()).thenReturn(false)
        whenever(getContactFromEmailUseCase(EMAIL, false)).thenReturn(createContactItem())
        whenever(getChatRoomByUserUseCase(USER_HANDLE)).thenReturn(chatRoom)

        underTest.uiState.test {
            assertThat(awaitLoadedState().email).isEqualTo(EMAIL)
        }
    }

    private suspend fun ReceiveTurbine<ContactInfoUiState>.awaitLoadedState(): ContactInfoUiState.Data {
        var item = awaitItem()
        while (item !is ContactInfoUiState.Data) {
            item = awaitItem()
        }
        return item
    }

    private fun createContactItem(
        alias: String? = ALIAS,
        fullName: String? = FULL_NAME,
    ) = ContactItem(
        handle = USER_HANDLE,
        email = EMAIL,
        contactData = ContactData(
            fullName = fullName,
            alias = alias,
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        defaultAvatarColor = null,
        visibility = UserVisibility.Visible,
        timestamp = 0L,
        areCredentialsVerified = false,
        status = UserChatStatus.Online,
        lastSeen = null,
        chatroomId = null,
    )

    companion object {
        private const val EMAIL = "contact@mega.nz"
        private const val USER_HANDLE = 42L
        private const val CHAT_ID = 123L
        private const val ALIAS = "Ally"
        private const val FULL_NAME = "Alice Anderson"
    }
}
