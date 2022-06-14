package mega.privacy.android.app.di.featureflag

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class FeatureFlagModule {

    @Binds
    abstract fun bindGetFeatureFlag(useCase: DefaultGetFeatureFlag): GetFeatureFlag

    @Binds
    abstract fun bindInsertFeatureFlag(useCase: DefaultSetFeatureFlag): SetFeatureFlag

    @Binds
    abstract fun bindGetAllFeatureFlags(useCase: DefaultGetAllFeatureFlags): GetAllFeatureFlags

}