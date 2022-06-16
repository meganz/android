package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.repository.FeatureFlagRepository
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.presentation.settings.model.PreferenceResource

@Module
@InstallIn(SingletonComponent::class)
class QAModule {
    @Provides
    @IntoSet
    fun providePreferenceResource(): PreferenceResource =
        PreferenceResource(R.xml.preferences_qa_entry)

    @Provides
    fun provideSetFeatureFlag(repository: FeatureFlagRepository): SetFeatureFlag =
        SetFeatureFlag(repository::setFeature)

    @Provides
    fun provideGetAllFeatureFlags(featureFlagRepository: FeatureFlagRepository): GetAllFeatureFlags =
        GetAllFeatureFlags(featureFlagRepository::getAllFeatures)
}