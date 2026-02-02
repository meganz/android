package mega.privacy.android.domain.entity

/**
 * Different values for Valid Name when user tries to update a name in the app.
 * E.g.: Create a folder, rename a node, create a text file.
 */
enum class InvalidNameType {
    /**
     * When no name
     */
    BLANK_NAME,

    /**
     * When name contains some invalid characters
     */
    INVALID_NAME,

    /**
     * Same name already exists
     */
    NAME_ALREADY_EXISTS,

    /**
     * Name changed for file by removing extension
     */
    NO_EXTENSION,

    /**
     * Original Extension for file has been changed
     */
    DIFFERENT_EXTENSION,

    /**
     * When name is "."
     */
    DOT_NAME,

    /**
     * When name is ".."
     */
    DOUBLE_DOT_NAME,

    /**
     * Everything is good to change name
     */
    VALID
}