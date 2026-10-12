package mega.privacy.android.domain.usecase.agesignal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.agesignal.UserAgeComplianceStatus
import mega.privacy.android.domain.repository.agesignal.AgeSignalRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [AgeSignalUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgeSignalUseCaseTest {
    private lateinit var underTest: AgeSignalUseCase
    private val repository = mock<AgeSignalRepository>()

    @BeforeAll
    fun setUp() {
        underTest = AgeSignalUseCase(
            repository = repository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @ParameterizedTest(name = "when repository returns {0}, use case returns {0}")
    @EnumSource(UserAgeComplianceStatus::class)
    fun `test that use case returns the same status as repository`(
        expectedStatus: UserAgeComplianceStatus,
    ) = runTest {
        whenever(repository.fetchAgeSignal()).thenReturn(expectedStatus)

        val result = underTest()

        assertThat(result).isEqualTo(expectedStatus)
    }
}

