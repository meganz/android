package mega.privacy.android.domain.entity.node


/**
 * Node
 */
sealed interface Node {

    /**
     * Id
     */
    val id: NodeId

    /**
     * Name
     */
    val name: String

    /**
     * Parent id
     */
    val parentId: NodeId

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

