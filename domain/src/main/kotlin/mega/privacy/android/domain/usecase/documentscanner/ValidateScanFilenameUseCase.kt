package mega.privacy.android.domain.usecase.documentscanner

import mega.privacy.android.domain.entity.documentscanner.ScanFilenameValidationStatus
import javax.inject.Inject

/**
 * Use Case that validates a given scan filename and returns the corresponding validation status
 *
 * The filename is considered valid if:
 *
 * 1. The filename is not empty or only contains whitespaces,
 * 2. The base filename (without the expected file extension) is not empty or only contains whitespaces, and
 * 3. The filename does not contain any of the following invalid characters:
 *     1. A forward slash /
 *     2. A back slash \
 *     3. A colon :
 *     4. A question mark ?
 *     5. A double quote "
 *     6. An asterisk *
 *     7. A less than symbol <
 *     8. A greater than symbol >
 *     9. A pipe symbol |
 */
class ValidateScanFilenameUseCase @Inject constructor() {

    /**
     * Invocation function
     *
     * @param filename The filename to be validated
     * @param fileExtension The expected filename extension
     *
     * @return The appropriate validation status
     */
    operator fun invoke(filename: String, fileExtension: String): ScanFilenameValidationStatus {
        // Check if the filename is empty or only contains whitespaces
        if (filename.isBlank()) {
            return ScanFilenameValidationStatus.EmptyFilename
        }

        // Check if the filename ends with the expected file extension, and whether or not
        // removing the file extension results in a blank filename
        if (filename.endsWith(fileExtension) && filename.substring(
                0, filename.length - fileExtension.length
            ).isBlank()
        ) {
            return ScanFilenameValidationStatus.EmptyFilename
        }

        // Check if the filename contains any of the invalid characters
        val invalidCharactersPattern = Regex("""["*/:<>?|\\]""")
        if (invalidCharactersPattern.containsMatchIn(filename)) {
            return ScanFilenameValidationStatus.InvalidFilename
        }

        // If all checks pass, the filename is valid
        return ScanFilenameValidationStatus.ValidFilename
    }
}