package mega.privacy.android.domain.entity


/**
 * Node info
 */
sealed interface NodeInfo {

    /**
     * Id
     */
    val id: Long

    /**
     * Name
     */
    val name: String

    /**
     * Parent id
     */
    val parentId: Long

    /**
     * Base64id
     */
    val base64Id: String

    /**
     * Label
     */
    val label: Int

    /**
     * Has version
     */
    val hasVersion: Boolean

    /**
     * Is favourite
     */
    val isFavourite: Boolean

    /**
     * Is exported
     */
    val isExported: Boolean

    /**
     * Is taken down
     */
    val isTakenDown: Boolean
}

