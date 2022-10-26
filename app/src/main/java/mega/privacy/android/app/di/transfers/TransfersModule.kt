package mega.privacy.android.app.di.transfers

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.app.domain.usecase.AreAllUploadTransfersPaused
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.AreTransfersPaused
import mega.privacy.android.domain.usecase.DefaultMonitorTransfersSize
import mega.privacy.android.domain.usecase.GetNumPendingDownloadsNonBackground
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.IsCompletedTransfersEmpty
import mega.privacy.android.domain.usecase.MonitorTransfersSize

/**
 * Use cases to check on transfer status
 */
@Module
@InstallIn(ViewModelComponent::class, ServiceComponent::class)
class TransfersModule {

    @Provides
    fun provideAreTransfersPaused(transfersRepository: TransferRepository):
            AreTransfersPaused = AreTransfersPaused(transfersRepository::areTransfersPaused)

    @Provides
    fun provideGetNumPendingDownloadsNonBackground(transfersRepository: TransferRepository):
            GetNumPendingDownloadsNonBackground =
        GetNumPendingDownloadsNonBackground(transfersRepository::getNumPendingDownloadsNonBackground)

    @Provides
    fun provideGetNumPendingUploads(transfersRepository: TransferRepository): GetNumPendingUploads =
        GetNumPendingUploads(transfersRepository::getNumPendingUploads)

    @Provides
    fun provideGetNumPendingTransfers(transfersRepository: TransferRepository): GetNumPendingTransfers =
        GetNumPendingTransfers(transfersRepository::getNumPendingTransfers)

    @Provides
    fun provideIsCompletedTransfersEmpty(transfersRepository: TransferRepository): IsCompletedTransfersEmpty =
        IsCompletedTransfersEmpty(transfersRepository::isCompletedTransfersEmpty)

    @Provides
    fun provideAreAllTransfersPaused(transfersRepository: TransferRepository): AreAllTransfersPaused =
        AreAllTransfersPaused(transfersRepository::areAllTransfersPaused)

    @Provides
    fun provideAreAllUploadTransfersPaused(transfersRepository: TransferRepository): AreAllUploadTransfersPaused =
        AreAllUploadTransfersPaused(transfersRepository::areAllUploadTransfersPaused)

    /**
     * Provide monitor transfers size
     */
    @Provides
    fun provideMonitorTransfersSize(
        transfersRepository: TransferRepository
    ): MonitorTransfersSize =
        DefaultMonitorTransfersSize(transfersRepository)
}
