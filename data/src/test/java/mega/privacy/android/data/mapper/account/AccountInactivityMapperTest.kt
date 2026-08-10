package mega.privacy.android.data.mapper.account

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.LastPurgeEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

internal class AccountInactivityMapperTest {
    private lateinit var underTest: AccountInactivityMapper

    private val deviceGateway = mock<DeviceGateway>()

    @BeforeEach
    fun setUp() {
        underTest = AccountInactivityMapper(deviceGateway)
    }

    @AfterEach
    fun tearDown() {
        reset(deviceGateway)
    }

    @Test
    fun `test that an inactive-reason event maps the purge timestamp`() {
        whenever(deviceGateway.now).thenReturn(APR_1_2024_SECONDS * MILLIS_IN_SECOND)

        val result = underTest(
            createEvent(reason = REASON_INACTIVE, lastActiveTs = JAN_1_2024_SECONDS),
        )

        assertThat(result?.purgeTimestamp).isEqualTo(PURGE_TS)
    }

    @Test
    fun `test that inactivity months are calculated between last active time and now`() {
        // last active 2024-01-01, now 2024-04-01 -> exactly 3 months.
        whenever(deviceGateway.now).thenReturn(APR_1_2024_SECONDS * MILLIS_IN_SECOND)

        val result = underTest(
            createEvent(reason = REASON_INACTIVE, lastActiveTs = JAN_1_2024_SECONDS),
        )

        assertThat(result?.inactivityMonths).isEqualTo(3)
    }

    @Test
    fun `test that inactivity months are coerced to at least 1 when the span is under a month`() {
        whenever(deviceGateway.now).thenReturn((JAN_1_2024_SECONDS + ONE_HOUR_SECONDS) * MILLIS_IN_SECOND)

        val result = underTest(
            createEvent(reason = REASON_INACTIVE, lastActiveTs = JAN_1_2024_SECONDS),
        )

        assertThat(result?.inactivityMonths).isEqualTo(1)
    }

    @Test
    fun `test that a non-inactive reason event returns null`() {
        val result = underTest(
            createEvent(reason = REASON_NOT_INACTIVE, lastActiveTs = JAN_1_2024_SECONDS),
        )

        assertThat(result).isNull()
    }

    @Test
    fun `test that a null last active timestamp returns null`() {
        val result = underTest(
            createEvent(reason = REASON_INACTIVE, lastActiveTs = null),
        )

        assertThat(result).isNull()
    }

    private fun createEvent(
        reason: Int,
        lastActiveTs: Long?,
        ts: Long = PURGE_TS,
    ) = LastPurgeEvent(
        handle = 1L,
        ts = ts,
        reason = reason,
        warningTs = null,
        lastActiveTs = lastActiveTs,
    )

    private companion object {
        private const val REASON_INACTIVE = 4
        private const val REASON_NOT_INACTIVE = 1
        private const val MILLIS_IN_SECOND = 1000L
        private const val ONE_HOUR_SECONDS = 3600L
        private const val PURGE_TS = 1_700_000_000L
        private const val JAN_1_2024_SECONDS = 1_704_067_200L
        private const val APR_1_2024_SECONDS = 1_711_929_600L
    }
}
