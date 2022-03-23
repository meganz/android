package mega.privacy.android.app.usecase.exception

/**
 * Exception thrown for over quota states.
 */
class OverQuotaException: Throwable("Action cannot be performed. TAccount is in over quota.")