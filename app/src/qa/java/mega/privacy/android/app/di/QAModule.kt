package mega.privacy.android.app.di

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.R
import mega.privacy.android.app.data.gateway.DistributionGateway
import mega.privacy.android.app.data.gateway.FirebaseDistributionGateway
import mega.privacy.android.app.data.gateway.MotionSensorFacade
import mega.privacy.android.app.data.gateway.MotionSensorGateway
import mega.privacy.android.app.data.gateway.VibratorFacade
import mega.privacy.android.app.data.gateway.VibratorGateway
import mega.privacy.android.app.data.repository.DefaultFeatureFlagRepository
import mega.privacy.android.app.data.repository.DefaultQARepository
import mega.privacy.android.app.data.repository.DefaultShakeDetectorRepository
import mega.privacy.android.app.data.usecase.DefaultShakeDetector
import mega.privacy.android.app.domain.repository.QARepository
import mega.privacy.android.app.domain.repository.ShakeDetectorRepository
import mega.privacy.android.app.domain.usecase.GetAllFeatureFlags
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.app.domain.usecase.ShakeDetector
import mega.privacy.android.app.domain.usecase.UpdateApp
import mega.privacy.android.app.domain.usecase.VibrateDevice
import mega.privacy.android.app.presentation.featureflag.ShakeDetectorViewModel
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

    /**
     * Provides implementation of @ShakeDetectorRepository
     *
     * @param repository : DefaultShakeDetectorRepository
     * @return repository : DefaultShakeDetectorRepository
     */
    @Provides
    fun provideShakeDetectorRepository(repository: DefaultShakeDetectorRepository): ShakeDetectorRepository =
        repository

    /**
     * Provides implementation of @ShakeDetector
     *
     * @param repository : DefaultShakeDetectorRepository
     * @return ShakeDetector: Implementation class @DefaultShakeDetector
     */
    @Provides
    fun provideShakeDetectorUseCase(repository: DefaultShakeDetectorRepository): ShakeDetector =
        DefaultShakeDetector(repository)

    /**
     * Provides implementation of @VibrateDevice
     *
     * @param repository : DefaultShakeDetectorRepository
     * @return VibrateDevice : Invokes operator function of VibrateDevice which calls repository
     */
    @Provides
    fun provideVibrateDeviceUseCase(repository: DefaultShakeDetectorRepository): VibrateDevice =
        VibrateDevice(repository::vibrateDevice)

    /**
     * Provides MotionSensorGateway to interact with System SensorManager
     *
     * @param context: ApplicationContext
     * @return MotionSensorGateway: Implementation class @MotionSensorFacade
     */
    @Provides
    fun provideMotionSensorGateway(@ApplicationContext context: Context): MotionSensorGateway =
        MotionSensorFacade(context)

    /**
     * Provides instance of @VibratorFacade
     *
     * @param vibrator : Device Vibrator
     * @return VibratorGateway : Implementation class @VibratorFacade
     */
    @Provides
    fun provideVibratorGateway(vibrator: Vibrator): VibratorGateway = VibratorFacade(vibrator)

    /**
     * Provides @ShakeDetectorViewModel instance
     *
     * @param coroutineScope: CoroutineScope
     * @param ioDispatcher : CoroutineDispatcher
     * @param vibrateDevice : VibrateDevice UseCase
     * @param shakeDetector: ShakeDetector UseCase
     *
     * @return @ShakeDetectorViewModel instance
     */
    @Provides
    fun provideShakeDetectorViewModel(
        @ApplicationScope coroutineScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        vibrateDevice: VibrateDevice,
        shakeDetector: ShakeDetector,
    ): ShakeDetectorViewModel =
        ShakeDetectorViewModel(coroutineScope,
            ioDispatcher,
            vibrateDevice,
            shakeDetector)

    /**
     * Provides device vibrator
     *
     * @param context: Context
     * @return Vibrator: Vibrator
     */
    @Suppress("DEPRECATION")
    @Provides
    fun provideDeviceVibrator(@ApplicationContext context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                    as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
}