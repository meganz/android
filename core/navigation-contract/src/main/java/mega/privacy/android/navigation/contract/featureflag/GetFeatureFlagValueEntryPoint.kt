package mega.privacy.android.navigation.contract.featureflag

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase

/**
 * Entry point to access [GetFeatureFlagValueUseCase] from non-Hilt components.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface GetFeatureFlagValueEntryPoint {
    val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase
}
