package mega.privacy.android.domain.entity.transfer

/**
 * Transfer stage, related to folder controllers
 */
enum class TransferStage {
    /**
     * Stage none, refer MegaTransfer.STAGE_NONE
     */
    STAGE_NONE,

    /**
     * Stage scanning, refer MegaTransfer.STAGE_SCAN
     */
    STAGE_SCANNING,

    /**
     * Stage creating tree, refer MegaTransfer.STAGE_CREATE_TREE
     */
    STAGE_CREATING_TREE,

    /**
     * Stage transferring files, refer MegaTransfer.STAGE_TRANSFERRING_FILES
     */
    STAGE_TRANSFERRING_FILES,
}