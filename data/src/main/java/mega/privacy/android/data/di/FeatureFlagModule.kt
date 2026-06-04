package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import mega.privacy.android.data.featuretoggle.DataFeatures
import mega.privacy.android.data.featuretoggle.persisted.PersistedFeatureFlagValueProvider
import mega.privacy.android.data.featuretoggle.persisted.PersistentFeatureFlagMemoryCache
import mega.privacy.android.data.featuretoggle.remote.ABTestFeatureFlagValueProvider
import mega.privacy.android.data.featuretoggle.remote.ApiFeatureFlagProvider
import mega.privacy.android.data.gateway.featuretoggle.PersistedFeatureFlagSnapshotGateway
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.featuretoggle.qualifier.DefaultFeatureFlagProviders
import mega.privacy.android.domain.featuretoggle.qualifier.PersistedFeatures
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

    /**
     * Provide persisted feature flag value provider
     */
    @Binds
    @IntoSet
    abstract fun providePersistedFeatureFlagValueProvider(
        persistedFeatureFlagValueProvider: PersistedFeatureFlagValueProvider,
    ): @JvmSuppressWildcards FeatureFlagValueProvider

    /**
     * Provide persisted feature flag snapshot store
     */
    @Binds
    abstract fun providePersistedFeatureFlagSnapshotStore(
        cache: PersistentFeatureFlagMemoryCache,
    ): PersistedFeatureFlagSnapshotGateway

    companion object {

        /**
         * Provide the set of features whose values should be persisted to disk.
         */
        @Provides
        @ElementsIntoSet
        @PersistedFeatures
        fun provideApiFeaturesAsPersistedFeatures(): Set<@JvmSuppressWildcards Feature> =
            ApiFeatures.entries.toSet()

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

        @Provides
        @IntoSet
        @DefaultFeatureFlagProviders
        fun provideDataFeaturesAsDefaultFlagProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
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

        @Provides
        @IntoSet
        @DefaultFeatureFlagProviders
        fun provideSyncFeaturesAsDefaultFlagProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
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

        @Provides
        @IntoSet
        @DefaultFeatureFlagProviders
        fun provideDomainFeaturesAsDefaultFlagProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            DomainFeatures.Companion
    }
}
