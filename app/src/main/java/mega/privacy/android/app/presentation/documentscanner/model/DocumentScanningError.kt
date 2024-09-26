package mega.privacy.android.app.presentation.documentscanner.model

import mega.privacy.android.shared.resources.R as SharedR
import androidx.annotation.StringRes

/**
 * An enumeration of different Error Types that can occur when using the ML Document Kit scanner, to
 * be used in the UI Layer
 *
 * @property textRes the String Resource ID for the Error Type
 */
enum class DocumentScanningError(
    @StringRes val textRes: Int,
) {
    /**
     * The Device has insufficient memory (less than 1.7 GB total RAM)
     */
    InsufficientRAM(SharedR.string.document_scanning_error_type_insufficient_ram),

    /**
     * A different type of Error occurred
     */
    GenericError(SharedR.string.document_scanning_error_type_generic_error),
}