package mega.privacy.android.domain.usecase.offline

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsOfflineTransferUseCaseTest {

    private val isOfflinePathUseCase: IsOfflinePathUseCase = mock()
    private val transfer: Transfer = mock()

    private lateinit var underTest: IsOfflineTransferUseCase

    @BeforeAll
    fun setup() {
        underTest = IsOfflineTransferUseCase(isOfflinePathUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(isOfflinePathUseCase)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that invoke returns the result of is offline path use case for the transfer path`(
        expected: Boolean,
    ) = runTest {
        val path = "some path"
        whenever(transfer.localPath) doReturn (path)
        whenever(isOfflinePathUseCase(path)) doReturn expected
        assertThat(underTest(transfer)).isEqualTo(expected)
    }
}