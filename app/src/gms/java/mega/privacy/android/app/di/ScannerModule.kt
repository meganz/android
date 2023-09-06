package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.middlelayer.scanner.ScannerHandler
import mega.privacy.android.app.service.scanner.ScannerHandlerImpl

/**
 * Scanner module
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class ScannerModule {
    /**
     * Provide [ScannerHandler] implementation
     */
    @Binds
    abstract fun bindScannerHandler(implementation: ScannerHandlerImpl): ScannerHandler
}