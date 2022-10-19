package mega.privacy.android.domain.entity


/**
 * Folder
 */
interface Folder {
    /**
     * Is folder in the rubbish bin
     */
    val isInRubbishBin: Boolean

    /**
     * Is the folder an incoming share
     */
    val isIncomingShare: Boolean

    /**
     * Is the folder an outgoing share
     */
    val isShared: Boolean

    /**
     * Is the folder a pending outgoing share
     */
    val isPendingShare: Boolean

    /**
     * Device
     */
    val device: String?
}
