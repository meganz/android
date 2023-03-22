package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfer.BroadcastFailedTransfer
import mega.privacy.android.domain.usecase.transfer.BroadcastTransferOverQuota
import mega.privacy.android.domain.usecase.transfer.MonitorFailedTransfer
import mega.privacy.android.domain.usecase.transfer.MonitorTransferOverQuota

@Module
@DisableInstallInCheck
internal abstract class InternalTransferModule {
    companion object {
        @Provides
        fun provideBroadcastTransferOverQuota(repository: TransferRepository) =
            BroadcastTransferOverQuota(repository::broadcastTransferOverQuota)

        @Provides
        fun provideMonitorTransferOverQuota(repository: TransferRepository) =
            MonitorTransferOverQuota(repository::monitorTransferOverQuota)

        @Provides
        fun provideMonitorFailedTransfer(repository: TransferRepository) =
            MonitorFailedTransfer(repository::monitorFailedTransfer)

        @Provides
        fun provideBroadcastFailedTransfer(repository: TransferRepository) =
            BroadcastFailedTransfer(repository::broadcastFailedTransfer)
    }
}