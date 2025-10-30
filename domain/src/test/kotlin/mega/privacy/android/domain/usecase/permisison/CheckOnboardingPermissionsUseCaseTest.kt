package mega.privacy.android.domain.usecase.permisison

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.environment.IsFirstLaunchUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [CheckOnboardingPermissionsUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CheckOnboardingPermissionsUseCaseTest {

    private lateinit var underTest: CheckOnboardingPermissionsUseCase

    private val isFirstLaunchUseCase = mock<IsFirstLaunchUseCase>()
    private val hasNotificationPermissionUseCase = mock<HasNotificationPermissionUseCase>()
    private val hasMediaPermissionUseCase = mock<HasMediaPermissionUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = CheckOnboardingPermissionsUseCase(
            isFirstLaunchUseCase = isFirstLaunchUseCase,
            hasNotificationPermissionUseCase = hasNotificationPermissionUseCase,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isFirstLaunchUseCase,
            hasNotificationPermissionUseCase,
            hasMediaPermissionUseCase,
        )
    }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is true when first launch and missing permissions`() =
        runTest {
            whenever(isFirstLaunchUseCase()).thenReturn(true)
            whenever(hasNotificationPermissionUseCase()).thenReturn(false)
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isTrue()
            assertThat(result.onlyShowNotificationPermission).isFalse()
        }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is true when first launch and missing notification permission`() =
        runTest {
            whenever(isFirstLaunchUseCase()).thenReturn(true)
            whenever(hasNotificationPermissionUseCase()).thenReturn(false)
            whenever(hasMediaPermissionUseCase()).thenReturn(true)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isTrue()
            assertThat(result.onlyShowNotificationPermission).isTrue()
        }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is true when first launch and missing media permission`() =
        runTest {
            whenever(isFirstLaunchUseCase()).thenReturn(true)
            whenever(hasNotificationPermissionUseCase()).thenReturn(true)
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isTrue()
            assertThat(result.onlyShowNotificationPermission).isFalse()
        }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is false when not first launch`() = runTest {
        whenever(isFirstLaunchUseCase()).thenReturn(false)
        whenever(hasNotificationPermissionUseCase()).thenReturn(false)
        whenever(hasMediaPermissionUseCase()).thenReturn(false)

        val result = underTest()

        assertThat(result.requestPermissionsOnFirstLaunch).isFalse()
        assertThat(result.onlyShowNotificationPermission).isFalse()
    }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is false when first launch but all permissions granted`() =
        runTest {
            whenever(isFirstLaunchUseCase()).thenReturn(true)
            whenever(hasNotificationPermissionUseCase()).thenReturn(true)
            whenever(hasMediaPermissionUseCase()).thenReturn(true)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isFalse()
            assertThat(result.onlyShowNotificationPermission).isFalse()
        }

    @ParameterizedTest
    @MethodSource("providePermissionScenarios")
    fun `test that use case returns correct result for different permission scenarios`(
        isFirstTime: Boolean,
        hasNotificationPermission: Boolean,
        hasMediaPermissions: Boolean,
        expectedRequestPermissions: Boolean,
        expectedOnlyShowNotificationPermission: Boolean,
    ) = runTest {
        whenever(isFirstLaunchUseCase()).thenReturn(isFirstTime)
        whenever(hasNotificationPermissionUseCase()).thenReturn(hasNotificationPermission)
        whenever(hasMediaPermissionUseCase()).thenReturn(hasMediaPermissions)

        val result = underTest()

        assertThat(result.requestPermissionsOnFirstLaunch).isEqualTo(expectedRequestPermissions)
        assertThat(result.onlyShowNotificationPermission)
            .isEqualTo(expectedOnlyShowNotificationPermission)
    }

    companion object {
        @JvmStatic
        private fun providePermissionScenarios(): Stream<Arguments> {
            return Stream.of(
                // isFirstTime, hasNotification, hasMedia, expectedRequest, expectedOnlyNotification
                Arguments.of(true, false, false, true, false),
                Arguments.of(true, false, true, true, true),
                Arguments.of(true, true, false, true, false),
                Arguments.of(true, true, true, false, false),
                Arguments.of(false, false, false, false, false),
                Arguments.of(false, false, true, false, true),
                Arguments.of(false, true, false, false, false),
                Arguments.of(false, true, true, false, false),
            )
        }
    }
}

