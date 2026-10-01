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
class GetOverDiskQuotaWarningTimestampsUseCaseTest {
    private val accountRepository = mock<AccountRepository>()
    private lateinit var underTest: GetOverDiskQuotaWarningTimestampsUseCase

    @BeforeAll
    fun setUp() {
        underTest = GetOverDiskQuotaWarningTimestampsUseCase(accountRepository)
    }

    @Test
    fun `test that invoke returns the warning timestamps from the repository`() = runTest {
        val timestamps = listOf(1_700_000_000L, 1_700_086_400L)
        whenever(accountRepository.getOverDiskQuotaWarningTimestamps()).thenReturn(timestamps)
        assertThat(underTest()).isEqualTo(timestamps)
    }
}
