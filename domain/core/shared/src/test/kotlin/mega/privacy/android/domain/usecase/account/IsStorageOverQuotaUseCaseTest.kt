package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastStorageOverQuotaUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsStorageOverQuotaUseCaseTest {
    private lateinit var underTest: IsStorageOverQuotaUseCase

    private val accountRepository: AccountRepository = mock()
    private val broadcastStorageOverQuotaUseCase: BroadcastStorageOverQuotaUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = IsStorageOverQuotaUseCase(
            accountRepository,
            broadcastStorageOverQuotaUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            accountRepository,
            broadcastStorageOverQuotaUseCase
        )
    }

    @Test
    fun `test that storage reached maximum quota returns true`() = runTest {
        whenever(accountRepository.getUsedStorage()).thenReturn(10L)
        whenever(accountRepository.getMaxStorage()).thenReturn(10L)

        assertThat(underTest()).isTrue()
        verify(broadcastStorageOverQuotaUseCase).invoke(true)
    }

    @Test
    fun `test that storage exceeded the maximum quota returns true`() = runTest {
        whenever(accountRepository.getUsedStorage()).thenReturn(15L)
        whenever(accountRepository.getMaxStorage()).thenReturn(10L)

        assertThat(underTest()).isTrue()
        verify(broadcastStorageOverQuotaUseCase).invoke(true)
    }

    @Test
    fun `test that enough storage return false`() = runTest {
        whenever(accountRepository.getUsedStorage()).thenReturn(9L)
        whenever(accountRepository.getMaxStorage()).thenReturn(10L)

        assertThat(underTest()).isFalse()
        verify(broadcastStorageOverQuotaUseCase).invoke(false)
    }
}
