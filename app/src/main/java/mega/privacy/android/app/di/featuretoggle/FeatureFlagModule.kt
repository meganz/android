package mega.privacy.android.app.di.featuretoggle

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoSet
import mega.privacy.android.data.featuretoggle.file.FileFeatureFlagValueProvider
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.featuretoggle.qualifier.DefaultFeatureFlagProviders
import mega.privacy.android.feature_flags.ABTestFeatures
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.feature_flags.FirebaseABTestFeatures

/**
 * Feature flag module
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagModule {

    /**
     * Provide feature flag value provider
     *
     */
    @Binds
    @IntoSet
    abstract fun provideFileFeatureFlagValueProvider(fileFeatureFlagValueProvider: FileFeatureFlagValueProvider): @JvmSuppressWildcards FeatureFlagValueProvider

    companion object {
        /**
         * Provide features
         *
         * @return App features
         */
        @Provides
        @ElementsIntoSet
        fun provideFeatures(): Set<@JvmSuppressWildcards Feature> =
            AppFeatures.entries.toSet()

        /**
         * Provide remote features
         *
         * @return Remote features
         */
        @Provides
        @ElementsIntoSet
        fun provideRemoteFeatures(): Set<@JvmSuppressWildcards Feature> =
            ABTestFeatures.entries.toSet()

        /**
         * Provide firebase remote config features
         *
         * @return Firebase remote config features
         */
        @Provides
        @ElementsIntoSet
        fun provideFirebaseABTestFeatures(): Set<@JvmSuppressWildcards Feature> =
            FirebaseABTestFeatures.entries.toSet()

        /**
         * Provide api features
         *
         * @return Api features
         */
        @Provides
        @ElementsIntoSet
        fun provideApiFeatures(): Set<@JvmSuppressWildcards Feature> =
            ApiFeatures.entries.toSet()

        /**
         * Provide feature flag value provider
         *
         */
        @Provides
        @IntoSet
        fun provideFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            AppFeatures.Companion

        /**
         * Provide remote feature flag default value provider
         *
         */
        @Provides
        @IntoSet
        fun provideRemoteFeatureFlagDefaultValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            ABTestFeatures.Companion

        /**
         * Provide firebase remote config feature flag default value provider
         *
         */
        @Provides
        @IntoSet
        fun provideFirebaseABTestFeaturesFlagDefaultValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            FirebaseABTestFeatures.Companion

        /**
         * Provide api feature flag default value provider
         *
         */
        @Provides
        @IntoSet
        fun provideApiFeaturesFlagDefaultValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            ApiFeatures.Companion

        @Provides
        @IntoSet
        @DefaultFeatureFlagProviders
        fun provideAppFeaturesAsDefaultFlagProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            AppFeatures.Companion

        @Provides
        @IntoSet
        @DefaultFeatureFlagProviders
        fun provideAbTestFeaturesAsDefaultFlagProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            ABTestFeatures.Companion

        @Provides
        @IntoSet
        @DefaultFeatureFlagProviders
        fun provideFirebaseABTestFeaturesAsDefaultFlagProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            FirebaseABTestFeatures.Companion

        @Provides
        @IntoSet
        @DefaultFeatureFlagProviders
        fun provideApiFeaturesAsDefaultFlagProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            ApiFeatures.Companion
    }
}
