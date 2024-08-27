package mega.privacy.android.app.nav

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.navigation.DeeplinkHandler
import mega.privacy.android.navigation.DeeplinkProcessor
import mega.privacy.android.navigation.DefaultDeeplinkHandler
import javax.inject.Singleton

/**
 * Module to provide a deeplink handler to handle all the defined processors
 */
@Module
@InstallIn(SingletonComponent::class)
object DeeplinkModule {

    /**
     * Provide deeplink handler to handle all the defined processors
     */
    @Provides
    @Singleton
    fun providesDefaultDeeplinkHandler(
        processors: Set<@JvmSuppressWildcards DeeplinkProcessor>
    ): DeeplinkHandler = DefaultDeeplinkHandler(processors)
}