package mega.privacy.android.app.presentation.rubbishbin.model

/**
 * Enum class that specifies different behaviors for the "Restore" functionality in
 * Rubbish Bin
 */
enum class RestoreType {

    /**
     * Indicates that the selected Nodes should be restored back to where they came from
     */
    RESTORE,

    /**
     * Indicates that the User must select a destination to restore all selected Nodes. This is
     * used when any of the selected Nodes is a Backup Node
     */
    MOVE,
}