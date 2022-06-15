package mega.privacy.android.app.usecase.exception

/**
 * Exception thrown for foreign over quota nodes.
 */
class ForeignNodeException: Throwable("Action cannot be performed. The parent node is in over quota.")