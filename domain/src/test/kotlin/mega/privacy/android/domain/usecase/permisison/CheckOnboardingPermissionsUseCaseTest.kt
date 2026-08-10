package mega.privacy.android.domain.usecase.permisison

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
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

    private val hasNotificationPermissionUseCase = mock<HasNotificationPermissionUseCase>()
    private val hasMediaPermissionUseCase = mock<HasMediaPermissionUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = CheckOnboardingPermissionsUseCase(
            hasNotificationPermissionUseCase = hasNotificationPermissionUseCase,
            hasMediaPermissionUseCase = hasMediaPermissionUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            hasNotificationPermissionUseCase,
            hasMediaPermissionUseCase,
        )
    }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is true when missing permissions`() =
        runTest {
            whenever(hasNotificationPermissionUseCase()).thenReturn(false)
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isTrue()
            assertThat(result.onlyShowNotificationPermission).isFalse()
        }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is true when missing notification permission`() =
        runTest {
            whenever(hasNotificationPermissionUseCase()).thenReturn(false)
            whenever(hasMediaPermissionUseCase()).thenReturn(true)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isTrue()
            assertThat(result.onlyShowNotificationPermission).isTrue()
        }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is true when missing media permission`() =
        runTest {
            whenever(hasNotificationPermissionUseCase()).thenReturn(true)
            whenever(hasMediaPermissionUseCase()).thenReturn(false)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isTrue()
            assertThat(result.onlyShowNotificationPermission).isFalse()
        }

    @Test
    fun `test that requestPermissionsOnFirstLaunch is false when all permissions granted`() =
        runTest {
            whenever(hasNotificationPermissionUseCase()).thenReturn(true)
            whenever(hasMediaPermissionUseCase()).thenReturn(true)

            val result = underTest()

            assertThat(result.requestPermissionsOnFirstLaunch).isFalse()
            assertThat(result.onlyShowNotificationPermission).isFalse()
        }

    @ParameterizedTest
    @MethodSource("providePermissionScenarios")
    fun `test that use case returns correct result for different permission scenarios`(
        hasNotificationPermission: Boolean,
        hasMediaPermissions: Boolean,
        expectedRequestPermissions: Boolean,
        expectedOnlyShowNotificationPermission: Boolean,
    ) = runTest {
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
                // hasNotification, hasMedia, expectedRequest, expectedOnlyNotification
                Arguments.of(false, false, true, false),
                Arguments.of(false, true, true, true),
                Arguments.of(true, false, true, false),
                Arguments.of(true, true, false, false),
                Arguments.of(true, true, false, false),
            )
        }
    }
}

