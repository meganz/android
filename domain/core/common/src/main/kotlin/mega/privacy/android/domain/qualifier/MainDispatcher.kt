package mega.privacy.android.domain.qualifier

import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Main dispatcher
 * refer to [Dispatchers.Main]
 */
@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class MainDispatcher