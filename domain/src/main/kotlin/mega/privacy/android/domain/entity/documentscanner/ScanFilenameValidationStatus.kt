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
     * The file name is only a dot
     */
    DotFileName,

    /**
     * The file name is only a double dot
     */
    DoubleDotFileName,

    /**
     * The filename contains invalid characters
     */
    InvalidFilename,

    /**
     * The filename does not contain the specified file extension
     */
    MissingFilenameExtension,

    /**
     * The filename does not end with the expected file extension
     */
    IncorrectFilenameExtension,
}