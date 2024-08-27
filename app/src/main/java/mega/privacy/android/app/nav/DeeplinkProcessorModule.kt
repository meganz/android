package mega.privacy.android.app.nav

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.sync.navigation.SyncDeeplinkProcessor
import mega.privacy.android.navigation.DeeplinkProcessor

/**
 * Module to bind the deeplink processors
 */
@Module
@InstallIn(SingletonComponent::class)
interface DeeplinkProcessorModule {

    /**
     * Bind the deeplink processor for Sync feature
     *
     * @param syncDeeplinkProcessor [SyncDeeplinkProcessor]
     */
    @Binds
    @IntoSet
    fun bindSyncDeepLinkProcessors(
        syncDeeplinkProcessor: SyncDeeplinkProcessor
    ): DeeplinkProcessor
}