package mega.privacy.android.app.presentation.permissions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.permissions.model.Permission
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.notifications.SetNotificationPermissionShownUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class PermissionsViewModelTest {
    private lateinit var underTest: PermissionsViewModel

    private val defaultAccountRepository: AccountRepository = mock()
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val getThemeModeUseCase: GetThemeMode = mock()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val setNotificationPermissionShownUseCase: SetNotificationPermissionShownUseCase =
        mock()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            defaultAccountRepository,
            getFeatureFlagValueUseCase,
            setNotificationPermissionShownUseCase
        )
        Dispatchers.resetMain()
    }

    private fun init() {
        underTest = PermissionsViewModel(
            defaultAccountRepository = defaultAccountRepository,
            ioDispatcher = testDispatcher,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getThemeModeUseCase = getThemeModeUseCase,
            setNotificationPermissionShownUseCase = setNotificationPermissionShownUseCase,
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
        whenever(getThemeModeUseCase()).thenReturn(flowOf(value))

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
                    Permission.Read to false
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
                    Permission.Read to false,
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
                    Permission.Read to false,
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
}