package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferQuotaWarningTrigger
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.ZoneId

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetTransferQuotaWarningShownUseCaseTest {

    private lateinit var underTest: SetTransferQuotaWarningShownUseCase

    private val accountRepository = mock<AccountRepository>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetTransferQuotaWarningShownUseCase(
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
    fun `test that today is recorded for the trigger the warning was shown for`(
        trigger: TransferQuotaWarningTrigger,
    ) = runTest {
        underTest(trigger)

        verify(accountRepository).setTransferQuotaWarningShownDay(
            trigger = trigger,
            epochDay = TODAY,
        )
    }

    private companion object {
        const val NOW = 1_772_000_000_000L
        val TODAY: Long = Instant.ofEpochMilli(NOW)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()
    }
}
