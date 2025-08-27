package mega.privacy.mobile.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.mobile.home.navigation.HomeNavItem


@Module
@InstallIn(SingletonComponent::class)
class HomeModule {

    @Provides
    @IntoSet
    fun provideHomeNavItem(): MainNavItem = HomeNavItem()
}