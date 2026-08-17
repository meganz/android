package mega.privacy.android.feature.sync.domain.usecase.notification

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.NotificationDetails
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationMessage
import mega.privacy.android.feature.sync.domain.entity.SyncNotificationType
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import mega.privacy.android.feature.sync.domain.usecase.notifcation.DisplaySyncNotificationUseCase
import mega.privacy.android.feature.sync.domain.usecase.notifcation.SetSyncNotificationShownUseCase
import mega.privacy.android.feature.sync.ui.notification.SyncNotificationManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking

internal class DisplaySyncNotificationUseCaseTest {
    private val syncNotificationRepository: SyncNotificationRepository = mock()
    private val syncNotificationManager: SyncNotificationManager = mock()
    private val setSyncNotificationShownUseCase: SetSyncNotificationShownUseCase = mock()

    private lateinit var underTest: DisplaySyncNotificationUseCase

    private val notification = SyncNotificationMessage(
        title = 1,
        text = 2,
        syncNotificationType = SyncNotificationType.STALLED_ISSUE,
        notificationDetails = NotificationDetails(path = "/sync", errorCode = null),
    )

    @BeforeEach
    fun setUp() {
        reset(
            syncNotificationRepository,
            syncNotificationManager,
            setSyncNotificationShownUseCase,
        )
        syncNotificationRepository.stub {
            onBlocking { isNotificationDisplayed(notification) }.thenReturn(false)
        }
        syncNotificationManager.stub {
            onBlocking { show(notification) }.thenReturn(1234)
        }
        underTest = DisplaySyncNotificationUseCase(
            syncNotificationRepository = syncNotificationRepository,
            syncNotificationManager = syncNotificationManager,
            setSyncNotificationShownUseCase = setSyncNotificationShownUseCase,
        )
    }

    @Test
    fun `test that invoke displays and records a new notification`() = runTest {
        val result = underTest(notification)

        assertThat(result).isTrue()
        verify(syncNotificationManager).show(notification)
        verifyBlocking(setSyncNotificationShownUseCase) { invoke(notification, 1234) }
    }

    @Test
    fun `test that invoke skips an already displayed notification`() = runTest {
        syncNotificationRepository.stub {
            onBlocking { isNotificationDisplayed(notification) }.thenReturn(true)
        }

        val result = underTest(notification)

        assertThat(result).isFalse()
        verify(syncNotificationManager, never()).show(any())
        verifyNoSetNotification()
    }

    @Test
    fun `test that invoke serialises duplicate collectors`() = runTest {
        syncNotificationRepository.stub {
            onBlocking { isNotificationDisplayed(notification) }
                .thenReturn(false)
                .thenReturn(true)
        }

        val results = listOf(
            async { underTest(notification) },
            async { underTest(notification) },
        ).awaitAll()

        assertThat(results.count { it }).isEqualTo(1)
        verify(syncNotificationManager).show(notification)
    }

    @Test
    fun `test that invoke does not record when notification manager cannot post`() = runTest {
        syncNotificationManager.stub {
            onBlocking { show(notification) }.thenReturn(null)
        }

        val result = underTest(notification)

        assertThat(result).isFalse()
        verify(syncNotificationManager).show(notification)
        verifyNoSetNotification()
    }

    @Test
    fun `test that invoke cancels notification when recording fails`() = runTest {
        val failure = IllegalStateException("Unable to persist notification")
        setSyncNotificationShownUseCase.stub {
            onBlocking { invoke(notification, 1234) }.thenThrow(failure)
        }

        val outcome = runCatching { underTest(notification) }

        assertThat(outcome.exceptionOrNull()).isEqualTo(failure)
        verify(syncNotificationManager).cancelNotification(1234)
    }

    private fun verifyNoSetNotification() {
        verifyBlocking(setSyncNotificationShownUseCase, never()) { invoke(any(), any()) }
    }
}
