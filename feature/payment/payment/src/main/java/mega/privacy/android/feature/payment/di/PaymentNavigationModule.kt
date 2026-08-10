package mega.privacy.android.feature.payment.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.payment.navigation.UpgradeFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class PaymentNavigationModule {

    @Provides
    @IntoSet
    fun provideUpgradeFeatureDestination(destination: UpgradeFeatureDestination): FeatureDestination =
        destination
}
