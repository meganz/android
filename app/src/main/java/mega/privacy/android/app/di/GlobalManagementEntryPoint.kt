package mega.privacy.android.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler

/**
 * Entry point to resolve the global management singletons in classes that cannot use constructor
 * or field injection (legacy adapters, listeners and hand-instantiated classes).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlobalManagementEntryPoint {

    /**
     * The [ActivityLifecycleHandler] instance.
     */
    fun activityLifecycleHandler(): ActivityLifecycleHandler
}
