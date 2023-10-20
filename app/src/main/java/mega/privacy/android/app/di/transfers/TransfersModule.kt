package mega.privacy.android.app.di.transfers

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.transfers.notification.DefaultDownloadNotificationMapper
import mega.privacy.android.app.presentation.transfers.notification.DefaultOverQuotaNotificationBuilder
import mega.privacy.android.app.presentation.transfers.notification.DefaultTransfersFinishedNotificationMapper
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
     * Binds [OverQuotaNotificationBuilder] to its default implementation [DefaultOverQuotaNotificationBuilder]
     */
    @Binds
    abstract fun bindOverQuotaNotificationBuilder(builder: DefaultOverQuotaNotificationBuilder): OverQuotaNotificationBuilder

    /**
     * Binds [TransfersFinishedNotificationMapper] to its default implementation [DefaultTransfersFinishedNotificationMapper]
     */
    @Binds
    abstract fun bindTransfersFinishedNotificationMapper(builder: DefaultTransfersFinishedNotificationMapper): TransfersFinishedNotificationMapper
}
