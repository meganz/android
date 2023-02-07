package mega.privacy.android.app.di.featuretoggle

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import dagger.multibindings.IntoMap
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.data.featuretoggle.file.FileFeatureFlagValueProvider
import mega.privacy.android.data.qualifier.FeatureFlagPriorityKey
import mega.privacy.android.data.repository.MegaNodeRepository
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import mega.privacy.android.domain.usecase.DefaultGetFeatureFlagValue
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import mega.privacy.android.domain.usecase.SetSecureFlag

/**
 * Feature flag module
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagModule {

    /**
     * Bind get feature flag value
     *
     * @param useCase
     */
    @Binds
    abstract fun bindGetFeatureFlagValue(useCase: DefaultGetFeatureFlagValue): GetFeatureFlagValue

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
         * Provides [SetSecureFlag] implementation
         *
         * @param filesRepository [FilesRepository]
         * @return [SetSecureFlag]
         */
        @Provides
        fun provideSetSecureFlag(megaNodeRepository: MegaNodeRepository): SetSecureFlag =
            SetSecureFlag(megaNodeRepository::setSecureFlag)
    }
}