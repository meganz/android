package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.service.scanner.BarcodeScannerImpl
import mega.privacy.android.app.service.scanner.DocumentScannerHandlerImpl
import mega.privacy.android.core.nodecomponents.scanner.DocumentScannerHandler
import mega.privacy.android.core.sharedcomponents.scanner.BarcodeScanner

/**
 * Scanner module
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class ScannerModule {
    /**
     * Provide [BarcodeScanner] implementation
     */
    @Binds
    abstract fun bindBarcodeScanner(implementation: BarcodeScannerImpl): BarcodeScanner

    /**
     * Provide [DocumentScannerHandler] implementation
     */
    @Binds
    abstract fun bindDocumentScannerHandler(implementation: DocumentScannerHandlerImpl): DocumentScannerHandler
}
