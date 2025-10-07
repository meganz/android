package mega.privacy.android.app.fcm

import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FcmManagerTest {

    private val firebaseMessaging: FirebaseMessaging = mock()
    private val firebaseAnalytics: FirebaseAnalytics = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val applicationScope = TestScope(testDispatcher)

    private lateinit var fcmManager: FcmManager

    private val freeUsersTopic get() = "mega_android_${BuildConfig.BUILD_TYPE}_free_users"
    private val paidUsersTopic get() = "mega_android_${BuildConfig.BUILD_TYPE}_paid_users"
    private val allUsersTopic get() = "mega_android_${BuildConfig.BUILD_TYPE}_all_users"

    @BeforeEach
    fun setUp() {
        reset(firebaseMessaging, firebaseAnalytics)
        fcmManager = FcmManager(applicationScope, firebaseMessaging, firebaseAnalytics)
    }

    private fun setupMockForSubscribeAndUnsubscribe() {
        val mockTask: Task<Void> = mock()
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.exception).thenReturn(null)
        whenever(mockTask.isCanceled).thenReturn(false)
        whenever(mockTask.result).thenReturn(null)
        whenever(firebaseMessaging.subscribeToTopic(anyString())).thenReturn(mockTask)
        whenever(firebaseMessaging.unsubscribeFromTopic(anyString())).thenReturn(mockTask)
    }

    @Test
    fun `test that subscribeToAllUsersTopic subscribes to correct topic`() = runTest {
        val mockTask: Task<Void> = mock()
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.exception).thenReturn(null)
        whenever(mockTask.isCanceled).thenReturn(false)
        whenever(mockTask.result).thenReturn(null)
        whenever(firebaseMessaging.subscribeToTopic(allUsersTopic)).thenReturn(mockTask)

        fcmManager.subscribeToAllUsersTopic()

        testDispatcher.scheduler.advanceUntilIdle()
        verify(firebaseMessaging).subscribeToTopic(allUsersTopic)
    }

    @Test
    fun `test that subscribeToAccountTypeTopic with FREE account subscribes to free users topic and unsubscribes from paid users topic`() =
        runTest {
            setupMockForSubscribeAndUnsubscribe()

            fcmManager.subscribeToAccountTypeTopic(AccountType.FREE)

            testDispatcher.scheduler.advanceUntilIdle()
            verify(firebaseMessaging).subscribeToTopic(freeUsersTopic)
            verify(firebaseMessaging).unsubscribeFromTopic(paidUsersTopic)
        }

    @Test
    fun `test that subscribeToAccountTypeTopic with PRO_I account subscribes to paid users topic and unsubscribes from free users topic`() =
        runTest {
            setupMockForSubscribeAndUnsubscribe()

            fcmManager.subscribeToAccountTypeTopic(AccountType.PRO_I)

            testDispatcher.scheduler.advanceUntilIdle()
            verify(firebaseMessaging).subscribeToTopic(paidUsersTopic)
            verify(firebaseMessaging).unsubscribeFromTopic(freeUsersTopic)
        }

    @Test
    fun `test that subscribeToAccountTypeTopic with PRO_FLEXI account subscribes to paid users topic and unsubscribes from free users topic`() =
        runTest {
            setupMockForSubscribeAndUnsubscribe()

            fcmManager.subscribeToAccountTypeTopic(AccountType.PRO_FLEXI)

            testDispatcher.scheduler.advanceUntilIdle()
            verify(firebaseMessaging).subscribeToTopic(paidUsersTopic)
            verify(firebaseMessaging).unsubscribeFromTopic(freeUsersTopic)
        }

    @Test
    fun `test that subscribeToAccountTypeTopic with BUSINESS account subscribes to paid users topic and unsubscribes from free users topic`() =
        runTest {
            setupMockForSubscribeAndUnsubscribe()

            fcmManager.subscribeToAccountTypeTopic(AccountType.BUSINESS)

            testDispatcher.scheduler.advanceUntilIdle()
            verify(firebaseMessaging).subscribeToTopic(paidUsersTopic)
            verify(firebaseMessaging).unsubscribeFromTopic(freeUsersTopic)
        }

    @Test
    fun `test that subscribeToAccountTypeTopic with UNKNOWN account subscribes to free users topic and unsubscribes from paid users topic`() =
        runTest {
            setupMockForSubscribeAndUnsubscribe()

            fcmManager.subscribeToAccountTypeTopic(AccountType.UNKNOWN)

            testDispatcher.scheduler.advanceUntilIdle()
            verify(firebaseMessaging).subscribeToTopic(freeUsersTopic)
            verify(firebaseMessaging).unsubscribeFromTopic(paidUsersTopic)
        }

    @Test
    fun `test that subscribeToTopic calls FirebaseMessaging subscribeToTopic`() = runTest {
        val topic = "test_topic"
        val mockTask: Task<Void> = mock()
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.exception).thenReturn(null)
        whenever(mockTask.isCanceled).thenReturn(false)
        whenever(mockTask.result).thenReturn(null)
        whenever(firebaseMessaging.subscribeToTopic(anyString())).thenReturn(mockTask)

        fcmManager.subscribeToTopic(topic)

        testDispatcher.scheduler.advanceUntilIdle()
        verify(firebaseMessaging).subscribeToTopic(topic)
    }

    @Test
    fun `test that unsubscribeFromTopic calls FirebaseMessaging unsubscribeFromTopic`() =
        runTest {
            val topic = "test_topic"
            val mockTask: Task<Void> = mock()
            whenever(mockTask.isComplete).thenReturn(true)
            whenever(mockTask.exception).thenReturn(null)
            whenever(mockTask.isCanceled).thenReturn(false)
            whenever(mockTask.result).thenReturn(null)
            whenever(firebaseMessaging.unsubscribeFromTopic(anyString())).thenReturn(mockTask)

            fcmManager.unsubscribeFromTopic(topic)

            testDispatcher.scheduler.advanceUntilIdle()
            verify(firebaseMessaging).unsubscribeFromTopic(topic)
        }

    @Test
    fun `test that all paid account types subscribe to paid users topic`() = runTest {
        val paidAccountTypes = listOf(
            AccountType.PRO_LITE,
            AccountType.PRO_I,
            AccountType.PRO_II,
            AccountType.PRO_III,
            AccountType.PRO_FLEXI,
            AccountType.BUSINESS,
            AccountType.STARTER,
            AccountType.BASIC,
            AccountType.ESSENTIAL
        )
        setupMockForSubscribeAndUnsubscribe()

        paidAccountTypes.forEach { accountType ->
            fcmManager.subscribeToAccountTypeTopic(accountType)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        verify(firebaseMessaging, times(paidAccountTypes.size)).subscribeToTopic(paidUsersTopic)
        verify(firebaseMessaging, times(paidAccountTypes.size)).unsubscribeFromTopic(freeUsersTopic)
    }

    @Test
    fun `test that all free account types subscribe to free users topic`() = runTest {
        val freeAccountTypes = listOf(AccountType.FREE, AccountType.UNKNOWN)
        setupMockForSubscribeAndUnsubscribe()

        freeAccountTypes.forEach { accountType ->
            fcmManager.subscribeToAccountTypeTopic(accountType)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        verify(firebaseMessaging, times(freeAccountTypes.size)).subscribeToTopic(freeUsersTopic)
        verify(firebaseMessaging, times(freeAccountTypes.size)).unsubscribeFromTopic(paidUsersTopic)
    }

    @Test
    fun `test that setAccountTypeUserProperty sets paid user property for paid account types`() {
        val paidAccountTypes = listOf(
            AccountType.PRO_LITE,
            AccountType.PRO_I,
            AccountType.PRO_II,
            AccountType.PRO_III,
            AccountType.STARTER,
            AccountType.BASIC,
            AccountType.ESSENTIAL
        )

        paidAccountTypes.forEach { accountType ->
            fcmManager.setAccountTypeUserProperty(accountType)
        }

        verify(firebaseAnalytics, times(paidAccountTypes.size)).setAnalyticsCollectionEnabled(true)
        verify(firebaseAnalytics, times(paidAccountTypes.size)).setUserProperty(
            "account_type",
            "paid"
        )
    }

    @Test
    fun `test that setAccountTypeUserProperty sets paid user property for business account types`() {
        val businessAccountTypes = listOf(
            AccountType.PRO_FLEXI,
            AccountType.BUSINESS
        )

        businessAccountTypes.forEach { accountType ->
            fcmManager.setAccountTypeUserProperty(accountType)
        }

        verify(firebaseAnalytics, times(businessAccountTypes.size)).setAnalyticsCollectionEnabled(
            true
        )
        verify(firebaseAnalytics, times(businessAccountTypes.size)).setUserProperty(
            "account_type",
            "paid"
        )
    }

    @Test
    fun `test that setAccountTypeUserProperty sets free user property for free account types`() {
        val freeAccountTypes = listOf(
            AccountType.FREE,
            AccountType.UNKNOWN
        )

        freeAccountTypes.forEach { accountType ->
            fcmManager.setAccountTypeUserProperty(accountType)
        }

        verify(firebaseAnalytics, times(freeAccountTypes.size)).setAnalyticsCollectionEnabled(true)
        verify(firebaseAnalytics, times(freeAccountTypes.size)).setUserProperty(
            "account_type",
            "free"
        )
    }

    @Test
    fun `test that setAccountTypeUserProperty enables analytics collection`() {
        fcmManager.setAccountTypeUserProperty(AccountType.FREE)

        verify(firebaseAnalytics).setAnalyticsCollectionEnabled(true)
    }
}