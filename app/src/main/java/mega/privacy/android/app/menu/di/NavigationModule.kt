package mega.privacy.android.app.menu.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntKey
import dagger.multibindings.IntoMap
import dagger.multibindings.IntoSet
import mega.privacy.android.app.menu.navigation.AchievementsItem
import mega.privacy.android.app.menu.navigation.ContactsItem
import mega.privacy.android.app.menu.navigation.CurrentPlanItem
import mega.privacy.android.app.menu.navigation.DeviceCentreItem
import mega.privacy.android.app.menu.navigation.MegaPassItem
import mega.privacy.android.app.menu.navigation.MegaVpnItem
import mega.privacy.android.app.menu.navigation.MenuNavItem
import mega.privacy.android.app.menu.navigation.OfflineFilesItem
import mega.privacy.android.app.menu.navigation.RubbishBinItem
import mega.privacy.android.app.menu.navigation.SettingsItem
import mega.privacy.android.app.menu.navigation.SharedItemsItem
import mega.privacy.android.app.menu.navigation.StorageItem
import mega.privacy.android.app.menu.navigation.TransferItItem
import mega.privacy.android.app.menu.navigation.TransfersItem
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.NavDrawerItem

@Module
@InstallIn(SingletonComponent::class)
class NavigationModule {
    @Provides
    @IntoSet
    fun provideMenuNavItem(): MainNavItem = MenuNavItem()

    @Provides
    @IntoMap
    @IntKey(10)
    fun provideCurrentPlanItem(): NavDrawerItem = CurrentPlanItem

    @Provides
    @IntoMap
    @IntKey(20)
    fun provideStorageItem(): NavDrawerItem = StorageItem

    @Provides
    @IntoMap
    @IntKey(30)
    fun provideContactsItem(): NavDrawerItem = ContactsItem

    @Provides
    @IntoMap
    @IntKey(40)
    fun provideAchievementsItem(): NavDrawerItem = AchievementsItem

    @Provides
    @IntoMap
    @IntKey(50)
    fun provideSharedItemsItem(): NavDrawerItem = SharedItemsItem

    @Provides
    @IntoMap
    @IntKey(60)
    fun provideDeviceCentreItem(): NavDrawerItem = DeviceCentreItem

    @Provides
    @IntoMap
    @IntKey(70)
    fun provideTransfersItem(): NavDrawerItem = TransfersItem

    @Provides
    @IntoMap
    @IntKey(80)
    fun provideOfflineFilesItem(): NavDrawerItem = OfflineFilesItem

    @Provides
    @IntoMap
    @IntKey(90)
    fun provideRubbishBinItem(): NavDrawerItem = RubbishBinItem

    @Provides
    @IntoMap
    @IntKey(100)
    fun provideSettingsItem(): NavDrawerItem = SettingsItem

    @Provides
    @IntoMap
    @IntKey(110)
    fun provideMegaVpnItem(): NavDrawerItem = MegaVpnItem

    @Provides
    @IntoMap
    @IntKey(120)
    fun provideMegaPassItem(): NavDrawerItem = MegaPassItem

    @Provides
    @IntoMap
    @IntKey(130)
    fun provideTransferItItem(): NavDrawerItem = TransferItItem
}