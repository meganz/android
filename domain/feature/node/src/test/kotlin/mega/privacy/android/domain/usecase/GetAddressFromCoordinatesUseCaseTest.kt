package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.GeocoderRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAddressFromCoordinatesUseCaseTest {

    private val geocoderRepository: GeocoderRepository = mock()
    private lateinit var underTest: GetAddressFromCoordinatesUseCase

    @BeforeEach
    fun setUp() {
        underTest = GetAddressFromCoordinatesUseCase(geocoderRepository)
    }

    @Test
    fun `test that invoke returns the address line from the repository`() = runTest {
        whenever(geocoderRepository.getAddressLine(52.09, 5.12)).thenReturn("Utrecht, Netherlands")

        assertThat(underTest(52.09, 5.12)).isEqualTo("Utrecht, Netherlands")
    }

    @Test
    fun `test that invoke returns null when the repository cannot resolve the address`() = runTest {
        whenever(geocoderRepository.getAddressLine(0.0, 0.0)).thenReturn(null)

        assertThat(underTest(0.0, 0.0)).isNull()
    }
}
