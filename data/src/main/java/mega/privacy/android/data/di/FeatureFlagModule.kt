package mega.privacy.android.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import mega.privacy.android.data.featuretoggle.remote.ABTestFeatureFlagValueProvider
import mega.privacy.android.data.qualifier.FeatureFlagPriorityKey
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
}