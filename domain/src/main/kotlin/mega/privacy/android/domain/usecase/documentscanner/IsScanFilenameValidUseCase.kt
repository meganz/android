package mega.privacy.android.domain.usecase.documentscanner

import javax.inject.Inject

/**
 * Use Case that checks if the selected filename in the Scan Confirmation page is valid
 *
 * The filename is invalid if it is empty, only has whitespaces, or contains any of the following
 * characters:
 *
 *  1. A forward slash /
 *  2. A back slash \
 *  3. A colon :
 *  4. A question mark ?
 *  5. A double quote "
 *  6. An asterisk *
 *  7. A less than symbol <
 *  8. A greater than symbol >
 *  9. A pipe symbol |
 */
class IsScanFilenameValidUseCase @Inject constructor() {

    /**
     * Invocation function
     *
     * @param filename The filename to be checked
     * @return true if the filename does not contain any of the characters specified in the [Regex]
     */
    operator fun invoke(filename: String) =
        filename.isNotBlank() && !Regex("""["*/:<>?|\\]""").containsMatchIn(filename)
}