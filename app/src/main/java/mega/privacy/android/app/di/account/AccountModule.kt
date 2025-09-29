package mega.privacy.android.app.di.account

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.myaccount.widget.SingleCardExampleHomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidget

@Module
@InstallIn(SingletonComponent::class)
class AccountModule {

    @Provides
    @IntoSet
    fun provideAccountHomeWidget(provider: SingleCardExampleHomeWidget): HomeWidget =
        provider
}