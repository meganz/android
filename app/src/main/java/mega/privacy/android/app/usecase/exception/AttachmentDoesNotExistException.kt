package mega.privacy.android.app.usecase.exception

/**
 * Exception thrown when a message does not contain any attachment.
 */
class AttachmentDoesNotExistException: Throwable("The message does not contain any attachment.")