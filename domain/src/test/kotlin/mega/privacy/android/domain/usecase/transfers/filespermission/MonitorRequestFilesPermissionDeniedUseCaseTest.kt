package mega.privacy.android.domain.usecase.transfers.filespermission

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorRequestFilesPermissionDeniedUseCaseTest {

    private lateinit var underTest: MonitorRequestFilesPermissionDeniedUseCase

    private val transfersRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorRequestFilesPermissionDeniedUseCase(transfersRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository)
    }

    @Test
    fun `test that monitorRequestFilesPermissionDenied is invoked in repository when this is invoked and returns correctly`() =
        runTest {
            val flowResult = flowOf(true)
            whenever(transfersRepository.monitorRequestFilesPermissionDenied())
                .thenReturn(flowResult)

            underTest.invoke().test {
                Truth.assertThat(awaitItem()).isTrue()
                awaitComplete()
            }
        }
}