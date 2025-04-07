package mega.privacy.android.domain.usecase.transfers.previews

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTransferUniqueIdToCancelUseCaseTest {

    private lateinit var underTest: MonitorTransferTagToCancelUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = MonitorTransferTagToCancelUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(transferRepository)
    }

    @Test
    fun `test that repository method is called when the use case is invoked`() = runTest {
        val transferTag = 123
        val nullValue: Int? = null

        whenever(transferRepository.monitorTransferTagToCancel()) doReturn
                flowOf(transferTag, nullValue)

        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(transferTag)
            assertThat(awaitItem()).isEqualTo(nullValue)
            cancelAndIgnoreRemainingEvents()
        }
    }
}