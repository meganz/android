package mega.privacy.android.domain.usecase.business

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.BusinessRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorBusinessAccountExpiredUseCaseTest {

    private lateinit var underTest: MonitorBusinessAccountExpiredUseCase

    private val businessRepository = mock<BusinessRepository>()


    @BeforeAll
    fun setUp() {
        underTest = MonitorBusinessAccountExpiredUseCase(businessRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(businessRepository)
    }

    @Test
    fun `test that invoke returns repository business account expired flow`() =
        runTest {
            val flow = mock<Flow<Unit>>()
            whenever(businessRepository.monitorBusinessAccountExpired()).thenReturn(flow)
            assertThat(underTest()).isEqualTo(flow)
        }
}