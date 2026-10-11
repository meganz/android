package mega.privacy.android.domain.usecase.transfers.previews

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BroadcastTransferUniqueIdToCancelUseCaseTest {

    private lateinit var underTest: BroadcastTransferTagToCancelUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = BroadcastTransferTagToCancelUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest(name = " when tag is {0}")
    @ValueSource(ints = [123])
    @NullSource
    fun `test that repository invokes correct appEventGateway fun`(
        transferTag: Int?,
    ) = runTest {
        whenever(transferRepository.broadcastTransferTagToCancel(transferTag)) doReturn Unit

        underTest(transferTag)

        verify(transferRepository).broadcastTransferTagToCancel(transferTag)
    }
}