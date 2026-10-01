package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TimeSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsDevice24HourFormatUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsDevice24HourFormatUseCaseTest {

    private lateinit var underTest: IsDevice24HourFormatUseCase

    private val timeSystemRepository = mock<TimeSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsDevice24HourFormatUseCase(
            timeSystemRepository = timeSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(timeSystemRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that the correct device time hour format is returned when invoked`(expected: Boolean) =
        runTest {
            whenever(timeSystemRepository.is24HourFormat()).thenReturn(expected)
            assertThat(underTest()).isEqualTo(expected)
        }
}
