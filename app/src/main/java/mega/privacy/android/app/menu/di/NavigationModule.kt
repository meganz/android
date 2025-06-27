package mega.privacy.android.app.menu.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.menu.navigation.MenuNavItem
import mega.privacy.android.navigation.contract.MainNavItem

@Module
@InstallIn(SingletonComponent::class)
class NavigationModule {
    @Provides
    @IntoSet
    fun provideMenuNavItem(): MainNavItem = MenuNavItem()
}