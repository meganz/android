package mega.privacy.android.app.middlelayer.scanner

import mega.privacy.android.app.presentation.qrcode.model.ScanResult

/**
 * Scanner handler
 */
interface ScannerHandler {
    /**
     * Start the scanner
     */
    suspend fun scan(): ScanResult
}