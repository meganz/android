package mega.privacy.android.app.di.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.activities.destinations.LegacyCoreActivityFeatureGraph
import mega.privacy.android.app.appstate.content.navigation.PermissionFeatureDestination
import mega.privacy.android.app.nav.MediaPlayerIntentMapper
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactFeatureDestination
import mega.privacy.android.app.presentation.filestorage.FileStorageFeatureDestination
import mega.privacy.android.app.presentation.logout.LogoutFeatureDestination
import mega.privacy.android.app.presentation.notification.navigation.NotificationsFeatureDestination
import mega.privacy.android.app.presentation.zipbrowser.ZipBrowserFeatureDestination
import mega.privacy.android.core.nodecomponents.mapper.NodeContentUriIntentMapper
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class FeatureDestinationModule {

    @Provides
    @IntoSet
    fun provideLegacyCoreActivityFeatureDestination(
        nodeContentUriIntentMapper: NodeContentUriIntentMapper,
        mediaPlayerIntentMapper: MediaPlayerIntentMapper,
    ): FeatureDestination =
        LegacyCoreActivityFeatureGraph(nodeContentUriIntentMapper, mediaPlayerIntentMapper)

    @Provides
    @IntoSet
    fun provideFileContactFeatureDestination(): FeatureDestination = FileContactFeatureDestination()


    @Provides
    @IntoSet
    fun provideNotificationsFeatureDestination(): FeatureDestination =
        NotificationsFeatureDestination()

    @Provides
    @IntoSet
    fun provideLogoutFeatureDestination(): FeatureDestination =
        LogoutFeatureDestination()

    @Provides
    @IntoSet
    fun providePermissionFeatureDestination(): FeatureDestination =
        PermissionFeatureDestination()

    @Provides
    @IntoSet
    fun provideFileStorageFeatureDestination(): FeatureDestination = FileStorageFeatureDestination()

    @Provides
    @IntoSet
    fun provideZipBrowserFeatureDestination(): FeatureDestination = ZipBrowserFeatureDestination()
}