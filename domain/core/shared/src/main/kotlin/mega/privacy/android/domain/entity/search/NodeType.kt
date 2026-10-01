package mega.privacy.android.domain.entity.search

/**
 * Enum class representing the type of a node
 */
enum class NodeType {

    /**
     * Represents a file node
     */
    FILE,

    /**
     * Represents a folder node
     */
    FOLDER,

    /**
     * Unknown node type
     * When provided for search all node types will be considered
     */
    UNKNOWN
}