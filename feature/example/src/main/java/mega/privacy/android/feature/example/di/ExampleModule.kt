package mega.privacy.android.feature.example.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.example.navigation.ExampleFeatureDestination
import mega.privacy.android.feature.example.navigation.ExampleMainItem
import mega.privacy.android.feature.example.navigation.OtherExampleMainItem
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem

@Module
@InstallIn(SingletonComponent::class)
class ExampleModule {

    @Provides
    @IntoSet
    fun provideExampleFeatureDestination(): FeatureDestination = ExampleFeatureDestination()

    @Provides
    @IntoSet
    fun provideExampleMainNavItem(): MainNavItem = ExampleMainItem()

    @Provides
    @IntoSet
    fun provideExampleMainNavItem2(): MainNavItem = OtherExampleMainItem()
}