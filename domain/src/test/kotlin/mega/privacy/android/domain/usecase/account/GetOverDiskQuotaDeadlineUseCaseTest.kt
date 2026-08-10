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
class GetOverDiskQuotaDeadlineUseCaseTest {
    private val accountRepository = mock<AccountRepository>()
    private lateinit var underTest: GetOverDiskQuotaDeadlineUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetOverDiskQuotaDeadlineUseCase(accountRepository)
    }

    @Test
    fun `test that invoke returns the deadline timestamp from the repository`() = runTest {
        whenever(accountRepository.getOverDiskQuotaDeadline()).thenReturn(1_700_000_000L)
        assertThat(underTest()).isEqualTo(1_700_000_000L)
    }
}
