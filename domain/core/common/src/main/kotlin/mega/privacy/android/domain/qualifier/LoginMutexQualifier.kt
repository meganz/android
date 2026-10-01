package mega.privacy.android.domain.qualifier

import javax.inject.Qualifier

/**
 * Mutex required for login purposes.
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class LoginMutex