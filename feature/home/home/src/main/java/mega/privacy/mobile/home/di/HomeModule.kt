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
import mega.privacy.mobile.home.presentation.home.widget.continuewhereleftoff.ContinueWhereLeftOffWidget
import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaItem
import mega.privacy.mobile.home.presentation.home.widget.domore.DoMoreWithMegaWidget
import mega.privacy.mobile.home.presentation.home.widget.domore.item.AddContactDoMoreItem
import mega.privacy.mobile.home.presentation.home.widget.domore.item.AddSyncDoMoreItem
import mega.privacy.mobile.home.presentation.home.widget.domore.item.CameraUploadsDoMoreItem
import mega.privacy.mobile.home.presentation.home.widget.domore.item.CreateAlbumDoMoreItem
import mega.privacy.mobile.home.presentation.home.widget.domore.item.ScanDocumentDoMoreItem
import mega.privacy.mobile.home.presentation.home.widget.domore.item.ScheduleMeetingDoMoreItem
import mega.privacy.mobile.home.presentation.home.widget.recents.RecentsWidget
import mega.privacy.mobile.home.presentation.home.widget.viewedlinks.ViewedLinksWidget


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

    @Provides
    @IntoSet
    fun provideContinueWhereLeftOffWidget(widget: ContinueWhereLeftOffWidget): HomeWidget = widget

    @Provides
    @IntoSet
    fun provideViewedLinksWidget(widget: ViewedLinksWidget): HomeWidget = widget

    @Provides
    @IntoSet
    fun provideDoMoreWithMegaWidget(widget: DoMoreWithMegaWidget): HomeWidget = widget

    @Provides
    @IntoSet
    fun provideCameraUploadsDoMoreItem(item: CameraUploadsDoMoreItem): DoMoreWithMegaItem = item

    @Provides
    @IntoSet
    fun provideAddSyncDoMoreItem(item: AddSyncDoMoreItem): DoMoreWithMegaItem = item

    @Provides
    @IntoSet
    fun provideScanDocumentDoMoreItem(item: ScanDocumentDoMoreItem): DoMoreWithMegaItem = item

    @Provides
    @IntoSet
    fun provideCreateAlbumDoMoreItem(item: CreateAlbumDoMoreItem): DoMoreWithMegaItem = item

    @Provides
    @IntoSet
    fun provideAddContactDoMoreItem(item: AddContactDoMoreItem): DoMoreWithMegaItem = item

    @Provides
    @IntoSet
    fun provideScheduleMeetingDoMoreItem(item: ScheduleMeetingDoMoreItem): DoMoreWithMegaItem = item
}
