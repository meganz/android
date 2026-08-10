package mega.privacy.android.domain.usecase.home

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AppVersion
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldDisplayNewFeatureUseCaseTest {
    private val settingsRepository = mock<SettingsRepository>()
    private val environmentRepository = mock<EnvironmentRepository>()

    private lateinit var underTest: ShouldDisplayNewFeatureUseCase

    @BeforeAll
    fun setUp() {
        underTest = ShouldDisplayNewFeatureUseCase(
            settingsRepository = settingsRepository,
            environmentRepository = environmentRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(settingsRepository, environmentRepository)
    }

    @ParameterizedTest(name = "should display={2} for last={0}, current={1}")
    @MethodSource("provideTestCases")
    fun `test should display new feature`(
        last: AppVersion?,
        current: AppVersion,
        expected: Boolean,
    ) = runTest {
        whenever(environmentRepository.getAppVersion()).thenReturn(current)
        whenever(settingsRepository.getLastVersionNewFeatureShown()).thenReturn(last)

        assertThat(underTest()).isEqualTo(expected)
    }

    private fun provideTestCases() = Stream.of(
        // null last version → shown
        Arguments.of(null, AppVersion(major = 14, minor = 2, patch = 1), true),
        // 14.0 → 14.1: minor increases, same major → shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = null),
            AppVersion(major = 14, minor = 1, patch = null),
            true
        ),
        // 14.0 → 13.5: major decreases → not shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = null),
            AppVersion(major = 13, minor = 5, patch = null),
            false
        ),
        // 14.0 → 15.0: major increases → shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = null),
            AppVersion(major = 15, minor = 0, patch = null),
            true
        ),
        // 14.0.1 → 15.0: major increases → shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = 1),
            AppVersion(major = 15, minor = 0, patch = null),
            true
        ),
        // 14.0.1 → 14.1.0: major increases → shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = 1),
            AppVersion(major = 14, minor = 1, patch = 0),
            true
        ),
        // 14.0.1 → 14.0.0: same major+minor, patch decreases → not shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = 1),
            AppVersion(major = 14, minor = 0, patch = 0),
            false
        ),
        // 14.0.1 → 14.0.2: same major+minor, patch increases → not shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = 1),
            AppVersion(major = 14, minor = 0, patch = 2),
            false
        ),
        // 14.0 → 14.0.1: same major+minor, patch increases → not shown
        Arguments.of(
            AppVersion(major = 14, minor = 0, patch = null),
            AppVersion(major = 14, minor = 0, patch = 1),
            false
        ),
    )
}
