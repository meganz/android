package mega.privacy.android.core.nodecomponents.scanner

import androidx.annotation.StringRes
import mega.privacy.android.shared.resources.R

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
    InsufficientRAM(R.string.document_scanning_error_type_insufficient_ram),

    /**
     * A different type of Error occurred
     */
    GenericError(R.string.document_scanning_error_type_generic_error),
}