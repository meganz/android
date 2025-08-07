package mega.privacy.android.domain.exception

sealed class NodeNameException : RuntimeException("Node name exception")

/**
 * Exception thrown when folder name is empty or contains only whitespace
 */
class EmptyNodeNameException : NodeNameException()

/**
 * Exception thrown when a folder with the same name already exists
 */
class NodeNameAlreadyExistsException : NodeNameException()

/**
 * Exception thrown when folder name contains invalid characters
 */
class InvalidNodeNameException : NodeNameException()