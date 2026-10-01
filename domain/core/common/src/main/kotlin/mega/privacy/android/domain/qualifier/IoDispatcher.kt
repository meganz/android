package mega.privacy.android.domain.qualifier

import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Io dispatcher
 * refer to [Dispatchers.IO]
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class IoDispatcher