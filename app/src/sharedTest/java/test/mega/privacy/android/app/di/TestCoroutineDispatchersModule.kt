package test.mega.privacy.android.app.di

import android.os.AsyncTask
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import mega.privacy.android.app.di.*

/**
 * Test coroutine dispatchers module
 *
 * Provides coroutine dispatchers for instrumented tests.
 * background dispatchers are replaced with [AsyncTask.THREAD_POOL_EXECUTOR] as Espresso
 * does not currently handle Coroutine idling resources very well.
 *
 * [Bug Report](https://github.com/Kotlin/kotlinx.coroutines/issues/242)
 *
 */
@Suppress("Deprecation")
@TestInstallIn(
    replaces = [CoroutinesDispatchersModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestCoroutinesDispatchersModule {

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher =
        AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()

    @IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher =
        AsyncTask.THREAD_POOL_EXECUTOR.asCoroutineDispatcher()

    @MainDispatcher
    @Provides
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @MainImmediateDispatcher
    @Provides
    fun providesMainImmediateDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}