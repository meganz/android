package mega.privacy.android.app.di.features

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.features.CloudDriveFeature
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CloudDriveFeatureModule {

    @Provides
    @ElementsIntoSet
    fun provideCloudDriveFeatures(): Set<@JvmSuppressWildcards Feature> =
        CloudDriveFeature.entries.toSet()

    @Provides
    @Singleton
    fun provideCloudDriveFeatureFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
        CloudDriveFeature.Companion
}
