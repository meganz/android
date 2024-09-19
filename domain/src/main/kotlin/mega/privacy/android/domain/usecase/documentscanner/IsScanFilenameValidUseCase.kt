package mega.privacy.android.domain.usecase.documentscanner

import javax.inject.Inject

/**
 * Use Case that checks if the selected filename in the Scan Confirmation page is valid
 *
 * The filename is invalid if it is empty, only has whitespaces, or contains any of the following
 * characters:
 *
 * 1. A forward slash /
 * 2. A colon :
 * 3. A question mark ?
 * 4. A double quote "
 * 5. An asterisk *
 * 6. A less than symbol <
 * 7. A greater than symbol >
 * 8. A pipe symbol |
 */
class IsScanFilenameValidUseCase @Inject constructor() {

    /**
     * Invocation function
     *
     * @param filename The filename to be checked
     * @return true if the filename does not contain any of the characters specified in the [Regex]
     */
    operator fun invoke(filename: String) =
        filename.isNotBlank() && !Regex("""["*/:<>?|]""").containsMatchIn(filename)
}