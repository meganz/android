package mega.privacy.android.app.errors

/**
 * Custom Error to encapsulate cases when transfer quota is exceeded.
 */
class QuotaOverdueMegaError : RuntimeException("Transfer quota is exceeded")
