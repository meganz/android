package mega.privacy.android.app.di.transfers

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.app.domain.usecase.AreAllUploadTransfersPaused
import mega.privacy.android.app.domain.usecase.CancelTransfer
import mega.privacy.android.data.repository.TransfersRepository
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

    /**
     * Provides the [CancelTransfer] implementation
     */
    @Provides
    fun provideCancelTransfer(transfersRepository: TransfersRepository): CancelTransfer =
        CancelTransfer(transfersRepository::cancelTransfer)

    /**
     * Provides the [AreTransfersPaused] implementation
     */
    @Provides
    fun provideAreTransfersPaused(transfersRepository: TransferRepository):
            AreTransfersPaused = AreTransfersPaused(transfersRepository::areTransfersPaused)

    /**
     * Provides the [GetNumPendingDownloadsNonBackground] implementation
     */
    @Provides
    fun provideGetNumPendingDownloadsNonBackground(transfersRepository: TransferRepository):
            GetNumPendingDownloadsNonBackground =
        GetNumPendingDownloadsNonBackground(transfersRepository::getNumPendingDownloadsNonBackground)

    /**
     * Provides the [GetNumPendingUploads] implementation
     */
    @Provides
    fun provideGetNumPendingUploads(transfersRepository: TransferRepository): GetNumPendingUploads =
        GetNumPendingUploads(transfersRepository::getNumPendingUploads)

    /**
     * Provides the [GetNumPendingTransfers] implementation
     */
    @Provides
    fun provideGetNumPendingTransfers(transfersRepository: TransferRepository): GetNumPendingTransfers =
        GetNumPendingTransfers(transfersRepository::getNumPendingTransfers)

    /**
     * Provides the [IsCompletedTransfersEmpty] implementation
     */
    @Provides
    fun provideIsCompletedTransfersEmpty(transfersRepository: TransferRepository): IsCompletedTransfersEmpty =
        IsCompletedTransfersEmpty(transfersRepository::isCompletedTransfersEmpty)

    /**
     * Provides the [AreAllTransfersPaused] implementation
     */
    @Provides
    fun provideAreAllTransfersPaused(transfersRepository: TransferRepository): AreAllTransfersPaused =
        AreAllTransfersPaused(transfersRepository::areAllTransfersPaused)

    /**
     * Provides the [AreAllUploadTransfersPaused] implementation
     */
    @Provides
    fun provideAreAllUploadTransfersPaused(transfersRepository: TransferRepository): AreAllUploadTransfersPaused =
        AreAllUploadTransfersPaused(transfersRepository::areAllUploadTransfersPaused)

    /**
     * Provides the [MonitorTransfersSize] implementation
     */
    @Provides
    fun provideMonitorTransfersSize(
        transfersRepository: TransferRepository,
    ): MonitorTransfersSize =
        DefaultMonitorTransfersSize(transfersRepository)
}
