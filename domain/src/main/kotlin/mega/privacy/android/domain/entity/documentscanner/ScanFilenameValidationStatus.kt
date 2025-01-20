package mega.privacy.android.domain.entity.documentscanner

/**
 * Enumeration class containing different statuses when validating the inputted scan filename
 */
enum class ScanFilenameValidationStatus {
    /**
     * The filename is valid
     */
    ValidFilename,

    /**
     * The filename is empty
     */
    EmptyFilename,

    /**
     * The filename contains invalid characters
     */
    InvalidFilename,
}