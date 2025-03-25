package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import mega.privacy.android.data.featuretoggle.DataFeatures
import mega.privacy.android.data.featuretoggle.remote.ABTestFeatureFlagValueProvider
import mega.privacy.android.data.featuretoggle.remote.ApiFeatureFlagProvider
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.shared.sync.featuretoggles.SyncFeatures

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FeatureFlagModule {

    /**
     * Provide api feature flag value provider
     *
     */
    @Binds
    @IntoSet
    abstract fun provideApiFeatureFlagValueProvider(apiFeatureFlagProvider: ApiFeatureFlagProvider): @JvmSuppressWildcards FeatureFlagValueProvider

    /**
     * Provide remote feature flag value provider
     *
     */
    @Binds
    @IntoSet
    abstract fun provideRemoteFeatureFlagValueProvider(ABTestFeatureFlagValueProvider: ABTestFeatureFlagValueProvider): @JvmSuppressWildcards FeatureFlagValueProvider

    companion object {
        /**
         * Provide Data features
         *
         * @return Data features
         */
        @Provides
        @ElementsIntoSet
        fun provideDataFeatures(): Set<@JvmSuppressWildcards Feature> =
            DataFeatures.entries.toSet()

        /**
         * Provide data feature flag value provider
         */
        @Provides
        @IntoSet
        fun provideDataFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            DataFeatures.Companion

        /**
         * Provide Sync features
         *
         * @return Sync features
         */
        @Provides
        @ElementsIntoSet
        fun provideSyncFeatures(): Set<@JvmSuppressWildcards Feature> =
            SyncFeatures.entries.toSet()

        /**
         * Provide sync feature flag value provider
         */
        @Provides
        @IntoSet
        fun provideSyncFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            SyncFeatures.Companion

        /**
         * Provide domain features
         *
         * @return Domain features
         */
        @Provides
        @ElementsIntoSet
        fun provideDomainFeatures(): Set<@JvmSuppressWildcards Feature> =
            DomainFeatures.entries.toSet()

        /**
         * Provide domain feature flag value provider
         */
        @Provides
        @IntoSet
        fun provideDomainFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            DomainFeatures.Companion
    }
}
