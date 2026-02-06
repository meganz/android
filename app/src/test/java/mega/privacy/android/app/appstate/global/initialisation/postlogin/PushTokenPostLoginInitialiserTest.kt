package mega.privacy.android.app.appstate.global.initialisation.postlogin

import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.utils.Constants.DEVICE_ANDROID
import mega.privacy.android.domain.usecase.pushnotifications.GetPushTokenUseCase
import mega.privacy.android.domain.usecase.pushnotifications.RegisterPushNotificationsUseCase
import mega.privacy.android.domain.usecase.pushnotifications.SetPushTokenUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PushTokenPostLoginInitialiserTest {

    private lateinit var underTest: PushTokenPostLoginInitialiser

    private val firebaseMessaging = mock<FirebaseMessaging>()
    private val getPushTokenUseCase = mock<GetPushTokenUseCase>()
    private val registerPushNotificationsUseCase = mock<RegisterPushNotificationsUseCase>()
    private val setPushTokenUseCase = mock<SetPushTokenUseCase>()
    private val ioDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setUp() {
        underTest = PushTokenPostLoginInitialiser(
            firebaseMessaging = firebaseMessaging,
            getPushTokenUseCase = getPushTokenUseCase,
            registerPushNotificationsUseCase = registerPushNotificationsUseCase,
            setPushTokenUseCase = setPushTokenUseCase,
            ioDispatcher = ioDispatcher,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            firebaseMessaging,
            getPushTokenUseCase,
            registerPushNotificationsUseCase,
            setPushTokenUseCase,
        )
    }

    @ParameterizedTest(name = "isFastLogin: {0}")
    @ValueSource(booleans = [false, true])
    fun `test that when token is new registers push notifications and sets token`(isFastLogin: Boolean) =
        runTest {
            val newToken = "new-fcm-token"
            val registeredToken = "registered-token"
            val tokenTask = mock<Task<String>>()
            whenever(tokenTask.isComplete).thenReturn(true)
            whenever(tokenTask.exception).thenReturn(null)
            whenever(tokenTask.isCanceled).thenReturn(false)
            whenever(tokenTask.result).thenReturn(newToken)
            whenever(firebaseMessaging.token).thenReturn(tokenTask)
            whenever(getPushTokenUseCase()).thenReturn("old-token")
            whenever(registerPushNotificationsUseCase(DEVICE_ANDROID, newToken)).thenReturn(
                registeredToken
            )

            underTest("session", isFastLogin)

            verify(registerPushNotificationsUseCase).invoke(DEVICE_ANDROID, newToken)
            verify(setPushTokenUseCase).invoke(registeredToken)
        }

    @Test
    fun `test that when token is null does not register push notifications`() = runTest {
        val tokenTask = mock<Task<String>>()
        whenever(tokenTask.isComplete).thenReturn(true)
        whenever(tokenTask.exception).thenReturn(null)
        whenever(tokenTask.isCanceled).thenReturn(false)
        whenever(tokenTask.result).thenReturn(null)
        whenever(firebaseMessaging.token).thenReturn(tokenTask)

        underTest("session", false)

        verifyNoInteractions(registerPushNotificationsUseCase)
        verifyNoInteractions(setPushTokenUseCase)
    }

    @Test
    fun `test that when token is empty does not register push notifications`() = runTest {
        val tokenTask = mock<Task<String>>()
        whenever(tokenTask.isComplete).thenReturn(true)
        whenever(tokenTask.exception).thenReturn(null)
        whenever(tokenTask.isCanceled).thenReturn(false)
        whenever(tokenTask.result).thenReturn("")
        whenever(firebaseMessaging.token).thenReturn(tokenTask)

        underTest("session", false)

        verifyNoInteractions(registerPushNotificationsUseCase)
        verifyNoInteractions(setPushTokenUseCase)
    }

    @Test
    fun `test that when token equals stored token does not register push notifications`() =
        runTest {
            val sameToken = "same-token"
            val tokenTask = mock<Task<String>>()
            whenever(tokenTask.isComplete).thenReturn(true)
            whenever(tokenTask.exception).thenReturn(null)
            whenever(tokenTask.isCanceled).thenReturn(false)
            whenever(tokenTask.result).thenReturn(sameToken)
            whenever(firebaseMessaging.token).thenReturn(tokenTask)
            whenever(getPushTokenUseCase()).thenReturn(sameToken)

            underTest("session", false)

            verifyNoInteractions(registerPushNotificationsUseCase)
            verifyNoInteractions(setPushTokenUseCase)
        }

    @Test
    fun `test that exception from registerPushNotificationsUseCase is caught and does not throw`() =
        runTest {
            val newToken = "new-fcm-token"
            val tokenTask = mock<Task<String>>()
            whenever(tokenTask.isComplete).thenReturn(true)
            whenever(tokenTask.exception).thenReturn(null)
            whenever(tokenTask.isCanceled).thenReturn(false)
            whenever(tokenTask.result).thenReturn(newToken)
            whenever(firebaseMessaging.token).thenReturn(tokenTask)
            whenever(getPushTokenUseCase()).thenReturn("old-token")
            whenever(registerPushNotificationsUseCase(any(), any())).thenThrow(
                RuntimeException("Register failed")
            )

            underTest("session", false)

            verify(registerPushNotificationsUseCase).invoke(DEVICE_ANDROID, newToken)
            verifyNoInteractions(setPushTokenUseCase)
        }

    @Test
    fun `test that exception from setPushTokenUseCase is caught and does not throw`() = runTest {
        val newToken = "new-fcm-token"
        val registeredToken = "registered-token"
        val tokenTask = mock<Task<String>>()
        whenever(tokenTask.isComplete).thenReturn(true)
        whenever(tokenTask.exception).thenReturn(null)
        whenever(tokenTask.isCanceled).thenReturn(false)
        whenever(tokenTask.result).thenReturn(newToken)
        whenever(firebaseMessaging.token).thenReturn(tokenTask)
        whenever(getPushTokenUseCase()).thenReturn("old-token")
        whenever(registerPushNotificationsUseCase(DEVICE_ANDROID, newToken)).thenReturn(
            registeredToken
        )
        whenever(setPushTokenUseCase(registeredToken)).thenThrow(RuntimeException("Set token failed"))

        underTest("session", false)

        verify(registerPushNotificationsUseCase).invoke(DEVICE_ANDROID, newToken)
        verify(setPushTokenUseCase).invoke(registeredToken)
    }

    @Test
    fun `test that exception from firebase token await is caught and does not throw`() = runTest {
        val tokenTask = mock<Task<String>>()
        whenever(tokenTask.isComplete).thenReturn(true)
        whenever(tokenTask.exception).thenReturn(Exception("Token failed"))
        whenever(tokenTask.isCanceled).thenReturn(false)
        whenever(firebaseMessaging.token).thenReturn(tokenTask)

        underTest("session", false)

        verifyNoInteractions(registerPushNotificationsUseCase)
        verifyNoInteractions(setPushTokenUseCase)
    }
}
