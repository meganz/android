package mega.privacy.android.app.presentation.contact

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.contacts.usecase.GetChatRoomUseCase
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.usecase.AreCredentialsVerified
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactUpdates
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.RequestLastGreen
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandle
import mega.privacy.android.domain.usecase.meeting.StartChatCall
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
    private lateinit var monitorStorageStateEvent: MonitorStorageStateEvent
    private lateinit var monitorConnectivity: MonitorConnectivity
    private lateinit var startChatCall: StartChatCall
    private lateinit var getChatRoomUseCase: GetChatRoomUseCase
    private lateinit var passcodeManagement: PasscodeManagement
    private lateinit var chatApiGateway: MegaChatApiGateway
    private lateinit var cameraGateway: CameraGateway
    private lateinit var chatManagement: ChatManagement
    private lateinit var areCredentialsVerified: AreCredentialsVerified
    private lateinit var monitorContactUpdates: MonitorContactUpdates
    private lateinit var getUserOnlineStatusByHandle: GetUserOnlineStatusByHandle
    private lateinit var requestLastGreen: RequestLastGreen
    private val testHandle = 123456L

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
            monitorStorageStateEvent,
            monitorConnectivity,
            startChatCall,
            getChatRoomUseCase,
            passcodeManagement,
            chatApiGateway,
            cameraGateway,
            chatManagement,
            areCredentialsVerified,
            monitorContactUpdates,
            getUserOnlineStatusByHandle,
            requestLastGreen
        )
    }

    private fun initMock() {
        monitorStorageStateEvent = mock()
        monitorConnectivity = mock()
        startChatCall = mock()
        getChatRoomUseCase = mock()
        passcodeManagement = mock()
        chatApiGateway = mock()
        cameraGateway = mock()
        chatManagement = mock()
        areCredentialsVerified = mock()
        monitorContactUpdates = mock()
        getUserOnlineStatusByHandle = mock()
        requestLastGreen = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial user state is Invalid`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertThat(initialState.userStatus).isEqualTo(UserStatus.Invalid)
            assertThat(initialState.lastGreen).isEqualTo(0)
            assertThat(initialState.userId).isNull()
            assertThat(initialState.email).isNull()
            assertThat(initialState.areCredentialsVerified).isFalse()
            assertThat(initialState.isCallStarted).isFalse()
        }
    }

    @Test
    fun `test that get user status and request last green does not trigger last green when user status is online`() =
        runTest {
            whenever(getUserOnlineStatusByHandle(testHandle)).thenReturn(UserStatus.Online)
            underTest.getUserStatusAndRequestForLastGreen(testHandle)
            underTest.state.test {
                assertThat(awaitItem().userStatus).isEqualTo(UserStatus.Online)
                verifyNoInteractions(requestLastGreen)
            }
        }

    @Test
    fun `test that get user status and request last green triggers last green when user status is away`() =
        runTest {
            whenever(getUserOnlineStatusByHandle(testHandle)).thenReturn(UserStatus.Away)
            underTest.getUserStatusAndRequestForLastGreen(testHandle)
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
}