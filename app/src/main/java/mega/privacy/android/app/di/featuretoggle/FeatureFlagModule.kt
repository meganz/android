package mega.privacy.android.app.di.featuretoggle

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoMap
import mega.privacy.android.app.featuretoggle.ABTestFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.data.featuretoggle.file.FileFeatureFlagValueProvider
import mega.privacy.android.data.qualifier.FeatureFlagPriorityKey
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

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
    @IntoMap
    @FeatureFlagPriorityKey(
        implementingClass = FileFeatureFlagValueProvider::class,
        priority = FeatureFlagValuePriority.ConfigurationFile
    )
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
            AppFeatures.values().toSet()

        /**
         * Provide remote features
         *
         * @return Remote features
         */
        @Provides
        @ElementsIntoSet
        fun provideRemoteFeatures(): Set<@JvmSuppressWildcards Feature> =
            ABTestFeatures.values().toSet()

        /**
         * Provide feature flag value provider
         *
         */
        @Provides
        @IntoMap
        @FeatureFlagPriorityKey(
            implementingClass = AppFeatures.Companion::class,
            priority = FeatureFlagValuePriority.Default
        )
        fun provideFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            AppFeatures.Companion

        /**
         * Provide remote feature flag default value provider
         *
         */
        @Provides
        @IntoMap
        @FeatureFlagPriorityKey(
            implementingClass = ABTestFeatures.Companion::class,
            priority = FeatureFlagValuePriority.Default
        )
        fun provideRemoteFeatureFlagDefaultValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
            ABTestFeatures.Companion
    }
}
