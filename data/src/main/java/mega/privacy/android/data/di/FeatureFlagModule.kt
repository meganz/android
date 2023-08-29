package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoMap
import mega.privacy.android.data.featuretoggle.DataFeatures
import mega.privacy.android.data.featuretoggle.remote.ABTestFeatureFlagValueProvider
import mega.privacy.android.data.qualifier.FeatureFlagPriorityKey
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeatureFlagModule {

    /**
     * Provide remote feature flag value provider
     *
     */
    @Binds
    @IntoMap
    @FeatureFlagPriorityKey(
        implementingClass = ABTestFeatureFlagValueProvider::class,
        priority = FeatureFlagValuePriority.RemoteToggled
    )
    abstract fun provideRemoteFeatureFlagValueProvider(ABTestFeatureFlagValueProvider: ABTestFeatureFlagValueProvider): @JvmSuppressWildcards FeatureFlagValueProvider

    companion object {
        /**
         * Provide features
         *
         * @return Data features
         */
        @Provides
        @ElementsIntoSet
        fun provideDataFeatures(): Set<@JvmSuppressWildcards Feature> =
            DataFeatures.values().toSet()

        /**
         * Provide data feature flag value provider
         */
        @Provides
        @IntoMap
        @FeatureFlagPriorityKey(
            implementingClass = DataFeatures.Companion::class,
            priority = FeatureFlagValuePriority.Default
        )
        fun provideDataFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            DataFeatures.Companion
    }
}
