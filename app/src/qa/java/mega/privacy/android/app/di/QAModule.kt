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
import mega.privacy.android.app.data.gateway.DistributionGateway
import mega.privacy.android.app.data.gateway.FirebaseDistributionGateway
import mega.privacy.android.app.data.repository.DefaultQARepository
import mega.privacy.android.app.domain.repository.QARepository
import mega.privacy.android.app.domain.usecase.UpdateApp
import mega.privacy.android.app.presentation.settings.model.PreferenceResource

/**
 * Provides dependencies used in the QA module
 *
 */
@Module
@InstallIn(SingletonComponent::class)
class QAModule {
    /**
     * QA only preferences
     *
     */
    @Provides
    @IntoSet
    fun bindPreferenceResource(): PreferenceResource =
        PreferenceResource(R.xml.preferences_qa_entry)

    /**
     * Provide SetFeatureFlag use case
     */
    @Provides
    fun provideSetFeatureFlag(repository: FeatureFlagRepository): SetFeatureFlag =
        SetFeatureFlag(repository::setFeature)

    /**
     * Provide GetAllFeatureFlags use case
     */
    @Provides
    fun provideGetAllFeatureFlags(featureFlagRepository: FeatureFlagRepository): GetAllFeatureFlags =
        GetAllFeatureFlags(featureFlagRepository::getAllFeatures)

    /**
     * Provide distribution gateway for QA builds
     *
     */
    @Provides
    fun provideDistributionGateway(): DistributionGateway = FirebaseDistributionGateway()

    /**
     * Provide qa repository
     *
     */
    @Provides
    fun provideQARepository(repository: DefaultQARepository): QARepository = repository

    /**
     * Provide update app use case
     *
     */
    @Provides
    fun provideUpdateApp(qaRepository: QARepository): UpdateApp =
        UpdateApp(qaRepository::updateApp)
}