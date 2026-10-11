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
class GetMaxUploadConnectionsUseCaseTest {
    private lateinit var underTest: GetMaxUploadConnectionsUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetMaxUploadConnectionsUseCase(
            transferRepository = transferRepository,
        )
    }

    @Test
    fun `test that getMaxUploadConnections in the repository is invoked and result is returned`() =
        runTest {
            val expected = 6
            whenever(transferRepository.getMaxUploadConnections()).thenReturn(expected)
            assertThat(underTest()).isEqualTo(expected)
        }
}
