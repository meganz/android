package mega.privacy.android.app.di.features

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.app.features.OrientationMigrationFeature
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider
import javax.inject.Singleton

/**
 * Dagger Hilt module for orientation migration feature dependencies.
 *
 * This module provides:
 * - Orientation migration feature flags
 * - Feature flag value provider for orientation migration
 *
 * Used for Android 16+ orientation compatibility on large screen devices.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object OrientationMigrationFeatureModule {

    /**
     * Provides the set of orientation migration features for the feature flag system.
     *
     * @return Set of orientation migration features
     */
    @Provides
    @ElementsIntoSet
    fun provideOrientationMigrationFeatures(): Set<@JvmSuppressWildcards Feature> =
        OrientationMigrationFeature.entries.toSet()

    /**
     * Provides the feature flag value provider for orientation migration.
     *
     * This provider handles the logic for determining when orientation migration
     * features should be enabled based on device capabilities and configuration.
     *
     * @return Feature flag value provider for orientation migration
     */
    @Provides
    @Singleton
    fun provideOrientationMigrationFlagValueProvider(): @JvmSuppressWildcards FeatureFlagValueProvider =
        OrientationMigrationFeature.Companion
}
