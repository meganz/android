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
import mega.privacy.android.app.presentation.transfers.notification.DefaultOverQuotaNotificationBuilder
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersActionGroupFinishNotificationBuilder
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersActionGroupProgressNotificationBuilder
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersFinishNotificationSummaryBuilder
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersFinishedNotificationMapper
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersNotificationMapper
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersProgressNotificationSummaryBuilder
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.mapper.transfer.ChatUploadNotificationMapper
import mega.privacy.android.data.mapper.transfer.OverQuotaNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupFinishNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersActionGroupProgressNotificationBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishNotificationSummaryBuilder
import mega.privacy.android.data.mapper.transfer.TransfersFinishedNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersNotificationMapper
import mega.privacy.android.data.mapper.transfer.TransfersProgressNotificationSummaryBuilder
import mega.privacy.android.domain.usecase.transfers.active.HandleChatTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleDownloadTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleUnverifiedBusinessAccountTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.active.HandleUploadTransferEventsUseCase
import mega.privacy.android.domain.usecase.transfers.active.IHandleTransferEventUseCase

/**
 * Module for transfers
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class TransfersModule {

    /**
     * Binds [TransfersNotificationMapper] to its default implementation [DefaultTransfersNotificationMapper]
     * @param mapper [DefaultTransfersNotificationMapper]
     * @return default [TransfersNotificationMapper]
     */
    @Binds
    abstract fun bindTransfersNotificationMapper(mapper: DefaultTransfersNotificationMapper): TransfersNotificationMapper

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

    /**
     * Binds [TransfersFinishNotificationSummaryBuilder] to its default implementation [DefaultTransfersFinishNotificationSummaryBuilder]
     */
    @Binds
    abstract fun bindTransfersFinishNotificationSummaryBuilder(builder: DefaultTransfersFinishNotificationSummaryBuilder): TransfersFinishNotificationSummaryBuilder

    /**
     * Binds [TransfersActionGroupFinishNotificationBuilder] to its default implementation [DefaultTransfersActionGroupFinishNotificationBuilder]
     */
    @Binds
    abstract fun bindTransfersGroupFinishNotificationBuilder(builder: DefaultTransfersActionGroupFinishNotificationBuilder): TransfersActionGroupFinishNotificationBuilder

    /**
     * Binds [TransfersProgressNotificationSummaryBuilder] to its default implementation [DefaultTransfersProgressNotificationSummaryBuilder]
     */
    @Binds
    abstract fun bindTransfersProgressNotificationSummaryBuilder(builder: DefaultTransfersProgressNotificationSummaryBuilder): TransfersProgressNotificationSummaryBuilder

    /**
     * Binds [TransfersActionGroupProgressNotificationBuilder] to its default implementation [DefaultTransfersActionGroupProgressNotificationBuilder]
     */
    @Binds
    abstract fun bindTransfersGroupProgressNotificationBuilder(builder: DefaultTransfersActionGroupProgressNotificationBuilder): TransfersActionGroupProgressNotificationBuilder


    /**
     * Binds [HandleTransferEventUseCase] to [IHandleTransferEventUseCase] set
     */
    @Binds
    @IntoSet
    abstract fun provideGeneralTransferHandle(useCase: HandleTransferEventUseCase): @JvmSuppressWildcards IHandleTransferEventUseCase

    /**
     * Binds [HandleUploadTransferEventsUseCase] to [IHandleTransferEventUseCase] set
     */
    @Binds
    @IntoSet
    abstract fun provideUploadTransferHandle(useCase: HandleUploadTransferEventsUseCase): @JvmSuppressWildcards IHandleTransferEventUseCase

    /**
     * Binds [HandleDownloadTransferEventsUseCase] to [IHandleTransferEventUseCase] set
     */
    @Binds
    @IntoSet
    abstract fun provideDownloadTransferHandle(useCase: HandleDownloadTransferEventsUseCase): @JvmSuppressWildcards IHandleTransferEventUseCase

    /**
     * Binds [HandleChatTransferEventsUseCase] to [IHandleTransferEventUseCase] set
     */
    @Binds
    @IntoSet
    abstract fun provideChatTransferHandle(useCase: HandleChatTransferEventsUseCase): @JvmSuppressWildcards IHandleTransferEventUseCase

    /**
     * Binds [HandleUnverifiedBusinessAccountTransferEventUseCase] to [IHandleTransferEventUseCase] set
     */
    @Binds
    @IntoSet
    abstract fun provideUnverifiedBusinessAccountTransferHandle(useCase: HandleUnverifiedBusinessAccountTransferEventUseCase): @JvmSuppressWildcards IHandleTransferEventUseCase

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
