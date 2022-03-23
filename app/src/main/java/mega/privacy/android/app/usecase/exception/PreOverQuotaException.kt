package mega.privacy.android.app.usecase.exception

/**
 * Exception thrown for pre over quota states.
 */
class PreOverQuotaException: Throwable("Action cannot be performed. Account is in pre over quota.")