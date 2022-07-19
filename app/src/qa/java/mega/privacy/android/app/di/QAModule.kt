package mega.privacy.android.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.R
import mega.privacy.android.app.data.gateway.DistributionGateway
import mega.privacy.android.app.data.gateway.FirebaseDistributionGateway
import mega.privacy.android.app.data.gateway.MotionSensorFacade
import mega.privacy.android.app.data.gateway.MotionSensorGateway
import mega.privacy.android.app.data.repository.DefaultQARepository
import mega.privacy.android.app.data.repository.DefaultShakeDetectorRepository
import mega.privacy.android.app.domain.repository.QARepository
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.domain.usecase.ShakeDetectorUseCase
import mega.privacy.android.app.domain.usecase.UpdateApp
import mega.privacy.android.app.domain.usecase.VibrateDeviceUseCase
import mega.privacy.android.app.presentation.settings.model.PreferenceResource
import mega.privacy.android.domain.repository.FeatureFlagRepository

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

    @Provides
    fun provideShakeDetectorRepository(repository: DefaultShakeDetectorRepository): ShakeDetectorRepository =
        repository

    @Provides
    fun provideShakeDetectorUseCase(repository: DefaultShakeDetectorRepository): ShakeDetectorUseCase =
        ShakeDetectorUseCase(repository::monitorShakeEvents)

    @Provides
    fun provideVibrateDeviceUseCase(repository: DefaultShakeDetectorRepository): VibrateDeviceUseCase =
        VibrateDeviceUseCase(repository::vibrateDevice)

    @Provides
    fun provideMotionSensorGateway(@ApplicationContext context: Context): MotionSensorGateway =
        MotionSensorFacade(context)
}