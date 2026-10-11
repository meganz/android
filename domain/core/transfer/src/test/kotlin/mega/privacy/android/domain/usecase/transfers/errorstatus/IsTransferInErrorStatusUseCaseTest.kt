package mega.privacy.android.domain.usecase.transfers.errorstatus

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsTransferInErrorStatusUseCaseTest {
    private lateinit var underTest: IsTransferInErrorStatusUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = IsTransferInErrorStatusUseCase(
            transferRepository,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(transferRepository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that use case returns repository current flow's value`(expected: Boolean) = runTest {
        whenever(transferRepository.monitorTransferInErrorStatus()) doReturn
                MutableStateFlow(expected)

        val actual = underTest()

        assertThat(actual).isEqualTo(expected)

    }
}