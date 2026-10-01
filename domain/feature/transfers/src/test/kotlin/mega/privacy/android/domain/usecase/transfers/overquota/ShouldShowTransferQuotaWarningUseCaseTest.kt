package mega.privacy.android.domain.usecase.transfers.overquota

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferQuotaWarningTrigger
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.ZoneId

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldShowTransferQuotaWarningUseCaseTest {

    private lateinit var underTest: ShouldShowTransferQuotaWarningUseCase

    private val accountRepository = mock<AccountRepository>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = ShouldShowTransferQuotaWarningUseCase(
            accountRepository = accountRepository,
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(accountRepository, getCurrentTimeInMillisUseCase)
        whenever(getCurrentTimeInMillisUseCase()).thenReturn(NOW)
    }

    @ParameterizedTest
    @EnumSource(TransferQuotaWarningTrigger::class)
    fun `test that the warning is shown when it has never been shown for the trigger`(
        trigger: TransferQuotaWarningTrigger,
    ) = runTest {
        whenever(accountRepository.getTransferQuotaWarningShownDay(trigger)).thenReturn(null)

        assertThat(underTest(trigger)).isTrue()
    }

    @ParameterizedTest
    @EnumSource(TransferQuotaWarningTrigger::class)
    fun `test that the warning is not shown when it has already been shown today`(
        trigger: TransferQuotaWarningTrigger,
    ) = runTest {
        whenever(accountRepository.getTransferQuotaWarningShownDay(trigger)).thenReturn(TODAY)

        assertThat(underTest(trigger)).isFalse()
    }

    @ParameterizedTest
    @EnumSource(TransferQuotaWarningTrigger::class)
    fun `test that the warning is shown again when the calendar day has rolled over`(
        trigger: TransferQuotaWarningTrigger,
    ) = runTest {
        whenever(accountRepository.getTransferQuotaWarningShownDay(trigger)).thenReturn(TODAY - 1)

        assertThat(underTest(trigger)).isTrue()
    }

    @Test
    fun `test that each trigger keeps its own allowance`() = runTest {
        whenever(
            accountRepository.getTransferQuotaWarningShownDay(TransferQuotaWarningTrigger.Download)
        ).thenReturn(TODAY)
        whenever(
            accountRepository.getTransferQuotaWarningShownDay(TransferQuotaWarningTrigger.Streaming)
        ).thenReturn(null)

        assertThat(underTest(TransferQuotaWarningTrigger.Download)).isFalse()
        assertThat(underTest(TransferQuotaWarningTrigger.Streaming)).isTrue()
    }

    private companion object {
        const val NOW = 1_772_000_000_000L
        val TODAY: Long = Instant.ofEpochMilli(NOW)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
    }
}
