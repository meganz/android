package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNumberOfNodesUseCaseTest {
    private val accountRepository = mock<AccountRepository>()
    private lateinit var underTest: GetNumberOfNodesUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetNumberOfNodesUseCase(accountRepository)
    }

    @Test
    fun `test that invoke returns the number of nodes from the repository`() = runTest {
        whenever(accountRepository.getNumberOfNodes()).thenReturn(42L)
        assertThat(underTest()).isEqualTo(42L)
    }
}
