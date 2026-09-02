package mega.privacy.android.feature.myaccount.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.myaccount.presentation.widget.MyAccountHomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidget

/**
 * Dependency injection module for MyAccount feature
 */

@Module
@InstallIn(SingletonComponent::class)
class MyAccountModule {

    @Provides
    @IntoSet
    fun provideMyAccountWidget(widget: MyAccountHomeWidget): HomeWidget = widget
}
