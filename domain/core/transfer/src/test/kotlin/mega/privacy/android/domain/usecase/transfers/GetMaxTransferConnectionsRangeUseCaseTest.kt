package mega.privacy.android.domain.usecase.transfers

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMaxTransferConnectionsRangeUseCaseTest {
    private lateinit var underTest: GetMaxTransferConnectionsRangeUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetMaxTransferConnectionsRangeUseCase()
    }

    @Test
    fun `test that getMaxTransferConnectionsRange in the repository is invoked and result is returned`() =
        runTest {
            val expected = 1..8
            assertThat(underTest()).isEqualTo(expected)
        }
}
