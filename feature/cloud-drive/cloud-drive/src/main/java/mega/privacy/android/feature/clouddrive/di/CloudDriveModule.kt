package mega.privacy.android.feature.clouddrive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.clouddrive.navigation.CloudDriveDeepLinkHandler
import mega.privacy.android.feature.clouddrive.navigation.CloudDriveFeatureDestination
import mega.privacy.android.feature.clouddrive.navigation.DriveSyncNavItem
import mega.privacy.android.feature.clouddrive.presentation.shares.links.navigation.PasswordLinkDeepLinkHandler
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.deeplinks.DeepLinkHandler

@Module
@InstallIn(SingletonComponent::class)
class CloudDriveModule {

    @Provides
    @IntoSet
    fun provideCloudDriveFeatureDestination(): FeatureDestination = CloudDriveFeatureDestination()


    @Provides
    @IntoSet
    fun provideDriveSyncNavItem(): MainNavItem = DriveSyncNavItem()

    @Provides
    @IntoSet
    fun provideCloudDriveDeepLinkHandler(handler: CloudDriveDeepLinkHandler): DeepLinkHandler =
        handler

    @Provides
    @IntoSet
    fun providePasswordLinkDeepLinkHandler(handler: PasswordLinkDeepLinkHandler): DeepLinkHandler =
        handler
}