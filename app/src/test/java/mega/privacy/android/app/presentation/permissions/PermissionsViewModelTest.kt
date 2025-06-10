package mega.privacy.android.app.presentation.permissions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.AnalyticsTestExtension
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.permissions.PermissionsViewModel.Companion.VIDEO_COMPRESSION_SIZE_LIMIT
import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.camerauploads.CheckEnableCameraUploadsStatusUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.notifications.SetNotificationPermissionShownUseCase
import mega.privacy.android.domain.usecase.photos.EnableCameraUploadsInPhotosUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.mobile.analytics.event.CameraUploadsEnabledEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PermissionsViewModelTest {
    private lateinit var underTest: PermissionsViewModel

    private val defaultAccountRepository: AccountRepository = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val setNotificationPermissionShownUseCase: SetNotificationPermissionShownUseCase =
        mock()
    private val checkEnableCameraUploadsStatusUseCase: CheckEnableCameraUploadsStatusUseCase =
        mock()
    private val startCameraUploadUseCase: StartCameraUploadUseCase = mock()
    private val enableCameraUploadsInPhotosUseCase: EnableCameraUploadsInPhotosUseCase = mock()
    private val testScope = TestScope(testDispatcher)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            defaultAccountRepository,
            getFeatureFlagValueUseCase,
            setNotificationPermissionShownUseCase,
            monitorThemeModeUseCase,
            checkEnableCameraUploadsStatusUseCase,
            startCameraUploadUseCase,
            enableCameraUploadsInPhotosUseCase
        )
        Dispatchers.resetMain()
    }

    private fun init() {
        underTest = PermissionsViewModel(
            defaultAccountRepository = defaultAccountRepository,
            ioDispatcher = testDispatcher,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
            setNotificationPermissionShownUseCase = setNotificationPermissionShownUseCase,
            checkEnableCameraUploadsStatusUseCase = checkEnableCameraUploadsStatusUseCase,
            startCameraUploadUseCase = startCameraUploadUseCase,
            enableCameraUploadsInPhotosUseCase = enableCameraUploadsInPhotosUseCase,
            applicationScope = testScope
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that onboarding flags are set correctly`(isEnabled: Boolean) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp))
            .thenReturn(isEnabled)

        init()

        underTest.setData(emptyList())

        underTest.uiState.test {
            assertThat(awaitItem().isOnboardingRevampEnabled).isEqualTo(isEnabled)
        }
    }

    @ParameterizedTest
    @EnumSource(ThemeMode::class)
    fun `test that theme mode is set correctly`(value: ThemeMode) = runTest {
        whenever(monitorThemeModeUseCase()).thenReturn(flowOf(value))

        init()

        underTest.uiState.test {
            assertThat(awaitItem().themeMode).isEqualTo(value)
        }
    }

    @Test
    fun `test that first visible permission should default to first missing permission`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp))
                .thenReturn(true)

            init()

            underTest.setData(
                permissions = listOf(
                    Permission.CameraBackup to false
                )
            )

            underTest.uiState.test {
                assertThat(awaitItem().visiblePermission).isEqualTo(NewPermissionScreen.CameraBackup)
            }
        }

    @Test
    fun `test that on next permission should update ui state`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp))
                .thenReturn(true)

            init()

            underTest.setData(
                permissions = listOf(
                    Permission.CameraBackup to false,
                    Permission.Notifications to false
                )
            )

            underTest.uiState.test {
                assertThat(awaitItem().visiblePermission).isEqualTo(NewPermissionScreen.CameraBackup)
                underTest.nextPermission()
                assertThat(awaitItem().visiblePermission).isEqualTo(NewPermissionScreen.Notification)
            }
        }

    @Test
    fun `test that on next permission should trigger finish event when null`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp))
                .thenReturn(true)

            init()

            underTest.setData(
                permissions = listOf(
                    Permission.CameraBackup to false,
                    Permission.Bluetooth to false
                )
            )

            underTest.uiState.test {
                assertThat(awaitItem().visiblePermission).isEqualTo(NewPermissionScreen.CameraBackup)
                underTest.nextPermission()
                assertThat(awaitItem().finishEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that on set permission shown should invoke use case`() = runTest {
        init()

        underTest.setPermissionPageShown()

        verify(setNotificationPermissionShownUseCase).invoke()
    }

    @Test
    fun `test that camera uploads should enable when media permission request granted`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp)).thenReturn(true)
        whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)

        init()

        underTest.setData(
            permissions = listOf(
                Permission.CameraBackup to false
            )
        )
        underTest.onMediaPermissionsGranted()

        verify(enableCameraUploadsInPhotosUseCase).invoke(
            shouldSyncVideos = false,
            shouldUseWiFiOnly = false,
            videoCompressionSizeLimit = VIDEO_COMPRESSION_SIZE_LIMIT,
            videoUploadQuality = VideoQuality.ORIGINAL
        )
    }

    @Test
    fun `test that camera upload should enable when status returns other than enable`() = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp)).thenReturn(true)
        whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT)

        init()

        underTest.setData(
            permissions = listOf(
                Permission.CameraBackup to false
            )
        )
        underTest.onMediaPermissionsGranted()

        verify(enableCameraUploadsInPhotosUseCase, never()).invoke(any(), any(), any(), any())
    }

    @Test
    fun `test that camera upload should not enable when checking status throws exception`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp)).thenReturn(true)
            whenever(checkEnableCameraUploadsStatusUseCase()).thenThrow(RuntimeException())

            init()

            underTest.setData(
                permissions = listOf(
                    Permission.CameraBackup to false
                )
            )
            underTest.onMediaPermissionsGranted()

            verify(enableCameraUploadsInPhotosUseCase, never()).invoke(any(), any(), any(), any())
        }

    @Test
    fun `test that when exception thrown on enabling camera upload should not track event`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.OnboardingRevamp)).thenReturn(true)
            whenever(checkEnableCameraUploadsStatusUseCase()).thenReturn(EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS)
            whenever(
                enableCameraUploadsInPhotosUseCase.invoke(
                    shouldSyncVideos = false,
                    shouldUseWiFiOnly = false,
                    videoCompressionSizeLimit = VIDEO_COMPRESSION_SIZE_LIMIT,
                    videoUploadQuality = VideoQuality.ORIGINAL
                )
            ).thenThrow(RuntimeException())

            init()

            underTest.setData(
                permissions = listOf(
                    Permission.CameraBackup to false
                )
            )

            underTest.onMediaPermissionsGranted()

            assertThat(analyticsExtension.events.contains(CameraUploadsEnabledEvent)).isFalse()
        }

    @Test
    fun `test that on start camera upload should invoke use cases`() = runTest {
        init()

        underTest.startCameraUploadIfGranted()

        verify(startCameraUploadUseCase).invoke()
    }

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }
}