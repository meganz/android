package mega.privacy.android.domain.exception

/**
 * Synchronisation exception - Thrown when expected state and actual state does not align
 *
 * @param message
 */
class SynchronisationException(message: String?) : RuntimeException(message)