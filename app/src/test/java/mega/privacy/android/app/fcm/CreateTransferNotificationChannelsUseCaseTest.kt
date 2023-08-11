package mega.privacy.android.app.fcm

import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import com.google.common.truth.Truth.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateTransferNotificationChannelsUseCaseTest {
    private lateinit var underTest: CreateTransferNotificationChannelsUseCase

    private val notificationManager = mock<NotificationManagerCompat>()

    @BeforeAll
    fun init() {
        underTest = CreateTransferNotificationChannelsUseCase(
            notificationManager = notificationManager
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(notificationManager)
    }

    @Test
    fun `test that download notifications channel is created with correct values when use case is invoked`() {
        underTest()
        verify(notificationManager).createNotificationChannelsCompat(org.mockito.kotlin.check { list ->
            val downloadChannel =
                list.first { it.id == CreateTransferNotificationChannelsUseCase.NOTIFICATION_CHANNEL_DOWNLOAD_ID }
            assertThat(downloadChannel.name)
                .isEqualTo(CreateTransferNotificationChannelsUseCase.NOTIFICATION_CHANNEL_DOWNLOAD_NAME)
            assertThat(downloadChannel.importance)
                .isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
            assertThat(downloadChannel.canShowBadge()).isFalse()
            assertThat(downloadChannel.sound).isNull()
        })
    }

    @Test
    fun `test that upload notifications channel is created with correct values when use case is invoked`() {
        underTest()
        verify(notificationManager).createNotificationChannelsCompat(org.mockito.kotlin.check { list ->
            val downloadChannel =
                list.first { it.id == CreateTransferNotificationChannelsUseCase.NOTIFICATION_CHANNEL_UPLOAD_ID }
            assertThat(downloadChannel.name)
                .isEqualTo(CreateTransferNotificationChannelsUseCase.NOTIFICATION_CHANNEL_UPLOAD_NAME)
            assertThat(downloadChannel.importance)
                .isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
            assertThat(downloadChannel.canShowBadge()).isFalse()
            assertThat(downloadChannel.sound).isNull()
        })
    }
}