package mega.privacy.android.app

import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import nz.mega.documentscanner.DocumentScannerActivity

/**
 * The activity is in order the UI issue of the document scanner in Android 15.
 */
class DocumentScannerEdgeToEdgeActivity : DocumentScannerActivity() {

    /**
     * Set up edge to edge for the activity.
     */
    override fun setUpEdgeToEdge() {
        super.setUpEdgeToEdge()
        enableEdgeToEdgeAndConsumeInsets()
    }
}