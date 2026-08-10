package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.transfers.InvalidMaxTransferConnectionsValueException
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetMaxUploadConnectionsUseCaseTest {
    private lateinit var underTest: SetMaxUploadConnectionsUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SetMaxUploadConnectionsUseCase(
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest(name = "when connections is {0}")
    @ValueSource(ints = [1, 6, 8])
    fun `test that setMaxUploadConnections in the repository is invoked when value is in range`(
        connections: Int,
    ) = runTest {
        underTest(connections)
        verify(transferRepository).setMaxUploadConnections(connections)
    }

    @ParameterizedTest(name = "when connections is {0}")
    @ValueSource(ints = [-1, 0, 9, 100])
    fun `test that invoke throws InvalidMaxTransferConnectionsValueException when value is out of range`(
        connections: Int,
    ) = runTest {
        assertThrows<InvalidMaxTransferConnectionsValueException> {
            underTest(connections)
        }
        verifyNoInteractions(transferRepository)
    }
}
