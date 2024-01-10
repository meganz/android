package mega.privacy.android.app.di.transfers

import android.app.NotificationManager
import com.google.common.truth.Truth.*
import mega.privacy.android.app.utils.Constants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransfersModuleTest {
    @Test
    fun `test that provided download notifications channel has correct values`() {
        val downloadChannel = TransfersModule.provideDownloadNotificationChannel()
        assertThat(downloadChannel.id)
            .isEqualTo(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID)
        assertThat(downloadChannel.name)
            .isEqualTo(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME)
        assertThat(downloadChannel.importance)
            .isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(downloadChannel.canShowBadge()).isFalse()
        assertThat(downloadChannel.sound).isNull()
    }

    @Test
    fun `test that provided upload notifications channel has correct values`() {
        val downloadChannel = TransfersModule.provideUploadNotificationChannel()
        assertThat(downloadChannel.id)
            .isEqualTo(Constants.NOTIFICATION_CHANNEL_UPLOAD_ID)
        assertThat(downloadChannel.name)
            .isEqualTo(Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME)
        assertThat(downloadChannel.importance)
            .isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(downloadChannel.canShowBadge()).isFalse()
        assertThat(downloadChannel.sound).isNull()
    }

    @Test
    fun `test that provided chat upload notifications channel has correct values`() {
        val downloadChannel = TransfersModule.provideChatUploadNotificationChannel()
        assertThat(downloadChannel.id)
            .isEqualTo(Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID)
        assertThat(downloadChannel.name)
            .isEqualTo(Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME)
        assertThat(downloadChannel.importance)
            .isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(downloadChannel.canShowBadge()).isFalse()
        assertThat(downloadChannel.sound).isNull()
    }
}