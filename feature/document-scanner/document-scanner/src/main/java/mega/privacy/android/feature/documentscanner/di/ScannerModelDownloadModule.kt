package mega.privacy.android.feature.documentscanner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.documentscanner.data.worker.WorkManagerScannerModelDownloadScheduler
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerModelDownloadScheduler

/**
 * Hilt binding for scheduling the background model download.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScannerModelDownloadModule {

    @Binds
    internal abstract fun bindScannerModelDownloadScheduler(
        impl: WorkManagerScannerModelDownloadScheduler,
    ): ScannerModelDownloadScheduler
}
