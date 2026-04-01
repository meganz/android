package mega.privacy.android.feature.documentscanner.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.documentscanner.data.repository.DefaultScanSessionRepository
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository

/**
 * Hilt module for binding scan session dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScanSessionModule {

    /**
     * Binds [DefaultScanSessionRepository] to [ScanSessionRepository].
     */
    @Binds
    abstract fun bindScanSessionRepository(
        impl: DefaultScanSessionRepository,
    ): ScanSessionRepository
}
