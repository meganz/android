package mega.privacy.android.domain.qualifier

import javax.inject.Qualifier

/**
 * A single-threaded dispatcher dedicated to synchronous SQLCipher / DatabaseHandler access.
 * Serialises blocking database calls so at most one thread contends for the connection-pool lock.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DatabaseDispatcher
