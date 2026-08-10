package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMoveLatestTargetUseCaseTest {
    private val accountRepository = mock<AccountRepository>()
    private val isNodeInRubbishOrDeletedUseCase = mock<IsNodeInRubbishOrDeletedUseCase>()
    private val underTest = GetMoveLatestTargetUseCase(
        accountRepository = accountRepository,
        isNodeInRubbishOrDeletedUseCase = isNodeInRubbishOrDeletedUseCase,
    )

    @BeforeEach
    fun reset() {
        reset(accountRepository, isNodeInRubbishOrDeletedUseCase)
    }

    @Test
    fun `test that invoke returns the path when it is not in the rubbish or deleted`() = runTest {
        val path = 1234L
        whenever(accountRepository.getLatestTargetMovePreference()).thenReturn(path)
        whenever(isNodeInRubbishOrDeletedUseCase(path)).thenReturn(false)
        assertThat(underTest()).isEqualTo(path)
    }

    @Test
    fun `test that invoke returns null when the path is in the rubbish or deleted`() = runTest {
        val path = 1234L
        whenever(accountRepository.getLatestTargetMovePreference()).thenReturn(path)
        whenever(isNodeInRubbishOrDeletedUseCase(path)).thenReturn(true)
        assertThat(underTest()).isNull()
    }

    @Test
    fun `test that invoke returns null when the repository returns null`() = runTest {
        whenever(accountRepository.getLatestTargetMovePreference()).thenReturn(null)
        assertThat(underTest()).isNull()
        verifyNoInteractions(isNodeInRubbishOrDeletedUseCase)
    }
}
