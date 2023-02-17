package mega.privacy.android.domain.entity.folderlink

/**
 * Enum class defining status of login call
 */
enum class FolderLoginStatus {
    /**
     * API_OK result from sdk
     */
    SUCCESS,

    /**
     * API_EINCOMPLETE result from sdk
     */
    API_INCOMPLETE,

    /**
     * API_EARGS result from sdk
     */
    INCORRECT_KEY,

    /**
     * Generic error
     */
    ERROR
}