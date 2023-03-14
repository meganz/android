package mega.privacy.android.app.di.transfers

import mega.privacy.android.domain.di.TransferModule as DomainTransferModule
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.AreAllTransfersPaused
import mega.privacy.android.app.domain.usecase.AreAllUploadTransfersPaused
import mega.privacy.android.app.domain.usecase.CancelAllUploadTransfers
import mega.privacy.android.app.domain.usecase.CancelTransfer
import mega.privacy.android.app.domain.usecase.StartUpload
import mega.privacy.android.data.repository.TransfersRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.AreTransfersPaused
import mega.privacy.android.domain.usecase.CancelTransferByTag
import mega.privacy.android.domain.usecase.DefaultHasPendingUploads
import mega.privacy.android.domain.usecase.DefaultMonitorTransfersSize
import mega.privacy.android.domain.usecase.GetNumPendingDownloadsNonBackground
import mega.privacy.android.domain.usecase.GetNumPendingTransfers
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.HasPendingUploads
import mega.privacy.android.domain.usecase.IsCompletedTransfersEmpty
import mega.privacy.android.domain.usecase.MonitorTransfersSize
import mega.privacy.android.domain.usecase.ResetTotalDownloads
import mega.privacy.android.domain.usecase.transfer.OngoingTransfersExist

/**
 * Use cases to check on transfer status
 */
@Module(includes = [DomainTransferModule::class])
@InstallIn(SingletonComponent::class, ViewModelComponent::class, ServiceComponent::class)
abstract class TransfersModule {

    /**
     * Binds the Use Case [HasPendingUploads] to its default implementation [DefaultHasPendingUploads]
     *
     * @param useCase [DefaultHasPendingUploads]
     * @return [HasPendingUploads]
     */
    @Binds
    abstract fun bindHasPendingUploads(useCase: DefaultHasPendingUploads): HasPendingUploads

    /**
     * Binds the Use Case [MonitorTransfersSize] to its default implementation [DefaultMonitorTransfersSize]
     *
     * @param useCase [DefaultMonitorTransfersSize]
     * @return [MonitorTransfersSize]
     */
    @Binds
    abstract fun bindMonitorTransfersSize(useCase: DefaultMonitorTransfersSize): MonitorTransfersSize

    companion object {

        /**
         * Provides the [CancelTransfer] implementation
         *
         * @param transfersRepository [TransfersRepository]
         * @return [CancelTransfer]
         */
        @Provides
        fun provideCancelTransfer(transfersRepository: TransfersRepository): CancelTransfer =
            CancelTransfer(transfersRepository::cancelTransfer)

        /**
         * Provides the [CancelAllUploadTransfers] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [CancelAllUploadTransfers]
         */
        @Provides
        fun provideCancelAllUploadTransfers(transfersRepository: TransferRepository): CancelAllUploadTransfers =
            CancelAllUploadTransfers(transfersRepository::cancelAllUploadTransfers)

        /**
         * Provides the [StartUpload] implementation
         *
         * @param transfersRepository [TransfersRepository]
         * @return [StartUpload]
         *
         */
        @Provides
        fun provideStartUpload(transfersRepository: TransfersRepository): StartUpload =
            StartUpload(transfersRepository::startUpload)

        /**
         * Provides the [AreTransfersPaused] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [AreTransfersPaused]
         */
        @Provides
        fun provideAreTransfersPaused(transfersRepository: TransferRepository):
                AreTransfersPaused = AreTransfersPaused(transfersRepository::areTransfersPaused)

        /**
         * Provides the [GetNumPendingDownloadsNonBackground] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [GetNumPendingDownloadsNonBackground]
         */
        @Provides
        fun provideGetNumPendingDownloadsNonBackground(transfersRepository: TransferRepository):
                GetNumPendingDownloadsNonBackground =
            GetNumPendingDownloadsNonBackground(transfersRepository::getNumPendingDownloadsNonBackground)

        /**
         * Provides the [GetNumPendingUploads] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [GetNumPendingUploads]
         */
        @Provides
        fun provideGetNumPendingUploads(transfersRepository: TransferRepository): GetNumPendingUploads =
            GetNumPendingUploads(transfersRepository::getNumPendingUploads)

        /**
         * Provides the [GetNumPendingTransfers] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [GetNumPendingTransfers]
         */
        @Provides
        fun provideGetNumPendingTransfers(transfersRepository: TransferRepository): GetNumPendingTransfers =
            GetNumPendingTransfers(transfersRepository::getNumPendingTransfers)

        /**
         * Provides the [IsCompletedTransfersEmpty] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [IsCompletedTransfersEmpty]
         */
        @Provides
        fun provideIsCompletedTransfersEmpty(transfersRepository: TransferRepository): IsCompletedTransfersEmpty =
            IsCompletedTransfersEmpty(transfersRepository::isCompletedTransfersEmpty)

        /**
         * Provides the [AreAllTransfersPaused] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [AreAllTransfersPaused]
         */
        @Provides
        fun provideAreAllTransfersPaused(transfersRepository: TransferRepository): AreAllTransfersPaused =
            AreAllTransfersPaused(transfersRepository::areAllTransfersPaused)

        /**
         * Provides the [AreAllUploadTransfersPaused] implementation
         *
         * @param transfersRepository [TransferRepository]
         * @return [AreAllUploadTransfersPaused]
         */
        @Provides
        fun provideAreAllUploadTransfersPaused(transfersRepository: TransferRepository): AreAllUploadTransfersPaused =
            AreAllUploadTransfersPaused(transfersRepository::areAllUploadTransfersPaused)

        /**
         * Provides the [CancelTransferByTag] implementation
         *
         * @param transferRepository [TransferRepository]
         */
        @Provides
        fun provideCancelTransferByTag(transferRepository: TransferRepository): CancelTransferByTag =
            CancelTransferByTag(transferRepository::cancelTransferByTag)

        /**
         * Provides the [ResetTotalDownloads] implementation
         *
         * @param transferRepository [TransferRepository]
         */
        @Provides
        fun provideResetTotalDownloads(transferRepository: TransferRepository):
                ResetTotalDownloads =
            ResetTotalDownloads(transferRepository::resetTotalDownloads)

        /**
         * Provides [OngoingTransfersExist].
         */
        @Provides
        fun provideOngoingTransfersExist(transferRepository: TransferRepository): OngoingTransfersExist =
            OngoingTransfersExist(transferRepository::ongoingTransfersExist)
    }
}
