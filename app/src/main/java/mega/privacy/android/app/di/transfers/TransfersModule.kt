package mega.privacy.android.app.di.transfers

import android.app.NotificationManager
import androidx.core.app.NotificationChannelCompat
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.transfers.notification.DefaultChatUploadNotificationMapper
import mega.privacy.android.app.presentation.transfers.notification.DefaultDownloadNotificationMapper
import mega.privacy.android.app.presentation.transfers.notification.DefaultOverQuotaNotificationBuilder
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersFinishedNotificationMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.data.mapper.transfer.DownloadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper

/**
 * Module for transfers
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TransfersModule {

    /**
     * Binds [DownloadNotificationMapper] to its default implementation [DefaultDownloadNotificationMapper]
     * @param mapper [DefaultDownloadNotificationMapper]
     * @return default [DownloadNotificationMapper]
     */
    @Binds
    abstract fun bindDownloadNotificationMapper(mapper: DefaultDownloadNotificationMapper): DownloadNotificationMapper

    /**
     * Binds [ChatUploadNotificationMapper] to its default implementation [DefaultChatUploadNotificationMapper]
     * @param mapper [DefaultChatUploadNotificationMapper]
     * @return default [ChatUploadNotificationMapper]
     */
    @Binds
    abstract fun bindChatUploadNotificationMapper(mapper: DefaultChatUploadNotificationMapper): ChatUploadNotificationMapper


    /**
     * Binds [OverQuotaNotificationBuilder] to its default implementation [DefaultOverQuotaNotificationBuilder]
     */
    @Binds
    abstract fun bindOverQuotaNotificationBuilder(builder: DefaultOverQuotaNotificationBuilder): OverQuotaNotificationBuilder

    /**
     * Binds [TransfersFinishedNotificationMapper] to its default implementation [DefaultTransfersFinishedNotificationMapper]
     */
    @Binds
    abstract fun bindTransfersFinishedNotificationMapper(builder: DefaultTransfersFinishedNotificationMapper): TransfersFinishedNotificationMapper

    companion object {

        /**
         * Provides download notification channel
         */
        @Provides
        @IntoSet
        fun provideDownloadNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_DOWNLOAD_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .setName(Constants.NOTIFICATION_CHANNEL_DOWNLOAD_NAME)
                .setShowBadge(false)
                .setSound(null, null)
                .build()

        /**
         * Provides upload notification channel
         */
        @Provides
        @IntoSet
        fun provideUploadNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_UPLOAD_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .setName(Constants.NOTIFICATION_CHANNEL_UPLOAD_NAME)
                .setShowBadge(false)
                .setSound(null, null)
                .build()

        /**
         * Provides chat upload notification channel
         */
        @Provides
        @IntoSet
        fun provideChatUploadNotificationChannel(): NotificationChannelCompat =
            NotificationChannelCompat.Builder(
                Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
                .setName(Constants.NOTIFICATION_CHANNEL_CHAT_UPLOAD_NAME)
                .setShowBadge(false)
                .setSound(null, null)
                .build()
    }
}
