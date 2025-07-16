package mega.privacy.android.feature.clouddrive.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.clouddrive.navigation.CloudDriveFeatureDestination
import mega.privacy.android.feature.clouddrive.navigation.CloudDriveMainNavItem
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem

@Module
@InstallIn(SingletonComponent::class)
class CloudDriveModule {

    @Provides
    @IntoSet
    fun provideCloudDriveFeatureDestination(): FeatureDestination = CloudDriveFeatureDestination()


    @Provides
    @IntoSet
    fun provideCloudDriveMainNavItem(): MainNavItem = CloudDriveMainNavItem()
}