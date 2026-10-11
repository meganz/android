package mega.privacy.android.domain.usecase.transfers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMaxDownloadConnectionsUseCaseTest {
    private lateinit var underTest: GetMaxDownloadConnectionsUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetMaxDownloadConnectionsUseCase(
            transferRepository = transferRepository,
        )
    }

    @Test
    fun `test that getMaxDownloadConnections in the repository is invoked and result is returned`() =
        runTest {
            val expected = 4
            whenever(transferRepository.getMaxDownloadConnections()).thenReturn(expected)
            assertThat(underTest()).isEqualTo(expected)
        }
}
