package mega.privacy.android.app.presentation.contact

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetChatRoomByUser
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.contact.ApplyContactUpdateForUser
import mega.privacy.android.domain.usecase.contact.GetContactFromChat
import mega.privacy.android.domain.usecase.contact.GetContactFromEmail
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandle
import mega.privacy.android.domain.usecase.contact.SetUserAlias
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ContactInfoViewModelTest {
    private lateinit var underTest: ContactInfoViewModel
    private lateinit var monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase
    private lateinit var monitorConnectivityUseCase: MonitorConnectivityUseCase
    private lateinit var startChatCall: StartChatCall
    private lateinit var getChatRoomUseCase: GetChatRoomUseCase
    private lateinit var passcodeManagement: PasscodeManagement
    private lateinit var chatApiGateway: MegaChatApiGateway
    private lateinit var cameraGateway: CameraGateway
    private lateinit var chatManagement: ChatManagement
    private lateinit var monitorContactUpdates: MonitorContactUpdates
    private lateinit var getUserOnlineStatusByHandle: GetUserOnlineStatusByHandle
    private lateinit var requestLastGreen: RequestLastGreen
    private lateinit var getChatRoom: GetChatRoom
    private lateinit var getContactFromEmail: GetContactFromEmail
    private lateinit var getContactFromChat: GetContactFromChat
    private lateinit var getChatRoomByUser: GetChatRoomByUser
    private lateinit var applyContactUpdateForUser: ApplyContactUpdateForUser
    private lateinit var setUserAlias: SetUserAlias
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)
    private val testHandle = 123456L
    private val testEmail = "test@gmail.com"
    private val contactData = ContactData(
        alias = "Iron Man",
        avatarUri = "https://avatar.uri.com",
        fullName = "Tony Stark",
    )
    private val contactItem = ContactItem(
        handle = testHandle,
        email = "test@gmail.com",
        contactData = contactData,
        defaultAvatarColor = "red",
        visibility = UserVisibility.Visible,
        timestamp = 123456789,
        areCredentialsVerified = true,
        status = UserStatus.Online,
        lastSeen = 0,
    )
    private val chatRoom = ChatRoom(
        chatId = 123456L,
        changes = null,
        title = "Chat title",
    )

    private val connectivityFlow: MutableStateFlow<Boolean> = MutableStateFlow(true)

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initMock()
        setDefaultMockValues()
        initViewModel()
    }

    private fun setDefaultMockValues() {
        whenever(monitorContactUpdates.invoke()).thenReturn(MutableSharedFlow())
    }

    private fun initViewModel() {
        underTest = ContactInfoViewModel(
            monitorStorageStateEventUseCase,
            monitorConnectivityUseCase,
            startChatCall,
            getChatRoomUseCase,
            passcodeManagement,
            chatApiGateway,
            cameraGateway,
            chatManagement,
            monitorContactUpdates,
            getUserOnlineStatusByHandle,
            requestLastGreen,
            getChatRoom,
            getChatRoomByUser,
            getContactFromChat,
            getContactFromEmail,
            applyContactUpdateForUser,
            setUserAlias,
            standardDispatcher,
        )
    }

    private fun initMock() {
        monitorStorageStateEventUseCase = mock()
        monitorConnectivityUseCase = mock()
        startChatCall = mock()
        getChatRoomUseCase = mock()
        passcodeManagement = mock()
        chatApiGateway = mock()
        cameraGateway = mock()
        chatManagement = mock()
        monitorContactUpdates = mock()
        getUserOnlineStatusByHandle = mock()
        requestLastGreen = mock()
        getChatRoom = mock()
        getChatRoomByUser = mock()
        getContactFromChat = mock()
        getContactFromEmail = mock()
        applyContactUpdateForUser = mock()
        setUserAlias = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun initUserInfo() {
        whenever(getContactFromEmail(email = testEmail, skipCache = true)).thenReturn(contactItem)
        whenever(getChatRoomByUser(contactItem.handle)).thenReturn(chatRoom)
        whenever(monitorConnectivityUseCase()).thenReturn(connectivityFlow)
        underTest.updateContactInfo(chatHandle = -1L, email = testEmail)
    }

    @Test
    fun `test that initial user state is Invalid`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.userStatus).isEqualTo(UserStatus.Invalid)
            assertThat(initialState.lastGreen).isEqualTo(0)
            assertThat(initialState.areCredentialsVerified).isFalse()
            assertThat(initialState.isCallStarted).isFalse()
        }
    }

    @Test
    fun `test that get user status and request last green does not trigger last green when user status is online`() =
        runTest {
            initUserInfo()
            whenever(getUserOnlineStatusByHandle(testHandle)).thenReturn(UserStatus.Online)
            underTest.getUserStatusAndRequestForLastGreen()
            underTest.state.test {
                assertThat(awaitItem().userStatus).isEqualTo(UserStatus.Online)
                verifyNoInteractions(requestLastGreen)
            }
        }

    @Test
    fun `test that get user status and request last green triggers last green when user status is away`() =
        runTest {
            initUserInfo()
            whenever(getUserOnlineStatusByHandle(anyLong())).thenReturn(UserStatus.Away)
            underTest.getUserStatusAndRequestForLastGreen()
            underTest.state.test {
                assertThat(awaitItem().userStatus).isEqualTo(UserStatus.Away)
                verify(requestLastGreen).invoke(userHandle = anyLong())
            }
        }

    @Test
    fun `test when update last green method is called state is updated with the last green value`() =
        runTest {
            whenever(getUserOnlineStatusByHandle(testHandle)).thenReturn(UserStatus.Online)
            underTest.updateLastGreen(testHandle, lastGreen = 5)
            underTest.state.test {
                val nextState = awaitItem()
                assertThat(nextState.userStatus).isEqualTo(UserStatus.Online)
                assertThat(nextState.lastGreen).isEqualTo(5)
            }
        }

    @Test
    fun `test when contact info screen launched from contacts emits title`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(connectivityFlow)
            whenever(getChatRoom(testHandle)).thenReturn(chatRoom)
            whenever(getContactFromChat(testHandle, skipCache = true)).thenReturn(contactItem)
            underTest.updateContactInfo(testHandle)
            underTest.state.test {
                val nextState = awaitItem()
                assertThat(nextState.primaryDisplayName).isEqualTo("Iron Man")
                assertThat(nextState.isFromContacts).isFalse()
                assertThat(nextState.email).isEqualTo("test@gmail.com")
            }
        }

    @Test
    fun `test when contact info screen launched from chats emits title`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(connectivityFlow)
            whenever(getContactFromEmail(testEmail, skipCache = true)).thenReturn(contactItem)
            whenever(getChatRoomByUser(testHandle)).thenReturn(chatRoom)
            underTest.updateContactInfo(-1L, testEmail)
            underTest.state.test {
                val nextState = awaitItem()
                assertThat(nextState.primaryDisplayName).isEqualTo("Iron Man")
                assertThat(nextState.isFromContacts).isTrue()
                assertThat(nextState.email).isEqualTo("test@gmail.com")
            }
        }

    @Test
    fun `test when new nickname is given the nick name added snack bar message is emitted`() =
        runTest {
            whenever(monitorConnectivityUseCase()).thenReturn(connectivityFlow)
            whenever(getContactFromEmail(testEmail, skipCache = true)).thenReturn(contactItem)
            whenever(getChatRoomByUser(testHandle)).thenReturn(chatRoom)
            whenever(setUserAlias("Spider Man", testHandle)).thenReturn("Spider Man")
            underTest.updateContactInfo(-1L, testEmail)
            underTest.state.test {
                val initialState = awaitItem()
                assertThat(initialState.primaryDisplayName).isEqualTo("Iron Man")
                assertThat(initialState.snackBarMessage).isNull()
            }
            underTest.updateNickName("Spider Man")
            underTest.state.test {
                val nextState = awaitItem()
                assertThat(nextState.snackBarMessage).isNotNull()
                assertThat(nextState.isFromContacts).isTrue()
                assertThat(nextState.email).isEqualTo("test@gmail.com")
            }
            underTest.onConsumeSnackBarMessageEvent()
            underTest.state.test {
                val nextState = awaitItem()
                assertThat(nextState.snackBarMessage).isNull()
            }
        }
}