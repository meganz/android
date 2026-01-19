package mega.privacy.mobile.home.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.navkey.MainNavItemNavKey
import mega.privacy.android.navigation.contract.qualifier.DefaultStartScreen
import mega.privacy.mobile.home.navigation.HomeFeatureGraph
import mega.privacy.mobile.home.navigation.HomeNavItem
import mega.privacy.mobile.home.presentation.home.widget.banner.BannerWidget
import mega.privacy.mobile.home.presentation.home.widget.chips.HomeChipsWidget
import mega.privacy.mobile.home.presentation.home.widget.recents.RecentsWidget


@Module
@InstallIn(SingletonComponent::class)
class HomeModule {

    @Provides
    @IntoSet
    fun provideHomeNavItem(): MainNavItem = HomeNavItem()

    @Provides
    @DefaultStartScreen
    fun provideDefaultStartScreen(): MainNavItemNavKey = HomeNavItem().destination

    @Provides
    @IntoSet
    fun provideHomeFeatureDestination(): FeatureDestination = HomeFeatureGraph()

    @Provides
    @IntoSet
    fun provideRecentsWidget(widget: RecentsWidget): HomeWidget = widget

    @Provides
    @IntoSet
    fun provideHomeChipsWidget(widget: HomeChipsWidget): HomeWidget = widget

    @Provides
    @IntoSet
    fun provideBannerWidget(widget: BannerWidget): HomeWidget = widget
}