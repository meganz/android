package mega.privacy.android.domain.usecase.transfers.errorstatus

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTransferInErrorStatusUseCaseTest {
    private lateinit var underTest: MonitorTransferInErrorStatusUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = MonitorTransferInErrorStatusUseCase(
            transferRepository,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(transferRepository)
    }

    @Test
    fun `test that use case returns repository flow`() = runTest {
        val expected = mock<StateFlow<Boolean>>()
        whenever(transferRepository.monitorTransferInErrorStatus()) doReturn expected
        val actual = underTest()

        assertThat(actual).isEqualTo(expected)
    }
}