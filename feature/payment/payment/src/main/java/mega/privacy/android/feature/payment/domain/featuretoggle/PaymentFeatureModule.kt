package mega.privacy.android.feature.payment.domain.featuretoggle

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PaymentFeatureModule {

    @Provides
    @ElementsIntoSet
    fun providePaymentFeatures(): Set<@JvmSuppressWildcards Feature> =
        PaymentFeatures.entries.toSet()


    @Provides
    @Singleton
    fun providePaymentFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
        PaymentFeatures.Companion
}
