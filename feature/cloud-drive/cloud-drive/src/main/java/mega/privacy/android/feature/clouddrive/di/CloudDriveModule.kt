package mega.privacy.android.feature.clouddrive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.clouddrive.navigation.CloudDriveFeatureDestination
import mega.privacy.android.feature.clouddrive.navigation.DriveSyncNavItem
import mega.privacy.android.feature.clouddrive.presentation.drivesync.DeviceCenterDeepLinkHandler
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
    @IntoMap
    @IntKey(20)
    fun provideDeviceCenterDeepLinkHandler(handler: DeviceCenterDeepLinkHandler): DeepLinkHandler = handler
}