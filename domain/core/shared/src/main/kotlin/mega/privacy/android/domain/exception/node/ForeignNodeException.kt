package mega.privacy.android.domain.exception.node

/**
 * Exception thrown for foreign over quota nodes.
 */
class ForeignNodeException :
    RuntimeException("Action cannot be performed. The parent node is in over quota.")