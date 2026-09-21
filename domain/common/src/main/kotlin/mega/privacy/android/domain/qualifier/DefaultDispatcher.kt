package mega.privacy.android.domain.qualifier

import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Default dispatcher
 * refer to [Dispatchers.Default]
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class DefaultDispatcher