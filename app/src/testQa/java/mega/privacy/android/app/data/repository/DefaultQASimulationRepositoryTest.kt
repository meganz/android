package mega.privacy.android.app.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.exception.MegaException
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * Runs with Robolectric so the real [org.json] implementation is used (the plain JVM
 * `android.jar` stub would otherwise make [JSONObject]/`JSONArray` return defaults), letting us
 * verify the `lastpurge` payload that `buildDevOptForPurge` produces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DefaultQASimulationRepositoryTest {

    private val megaApiGateway = mock<MegaApiGateway>()
    private val ioDispatcher = UnconfinedTestDispatcher()

    private lateinit var underTest: DefaultQASimulationRepository

    @Before
    fun setUp() {
        underTest = DefaultQASimulationRepository(
            megaApiGateway = megaApiGateway,
            ioDispatcher = ioDispatcher,
        )
    }

    @Test
    fun `test that setDevOptForPurge writes the full lastpurge payload when warning and last active are present`() =
        runTest {
            stubSetUserAttributeSuccess()
            val valueCaptor = argumentCaptor<String>()

            underTest.setDevOptForPurge(
                purgeTimestamp = PURGE_TS,
                reason = REASON,
                warningTimestamp = WARNING_TS,
                lastActiveTimestamp = LAST_ACTIVE_TS,
            )

            verify(megaApiGateway).setUserAttribute(
                eq(MegaApiJava.USER_ATTR_DEV_OPT),
                valueCaptor.capture(),
                any(),
            )
            val lastPurge = JSONObject(valueCaptor.firstValue).getJSONArray("lastpurge")
            assertThat(lastPurge.length()).isEqualTo(4)
            assertThat(lastPurge.getLong(0)).isEqualTo(PURGE_TS)
            assertThat(lastPurge.getInt(1)).isEqualTo(REASON)
            assertThat(lastPurge.getLong(2)).isEqualTo(WARNING_TS)
            assertThat(lastPurge.getLong(3)).isEqualTo(LAST_ACTIVE_TS)
        }

    @Test
    fun `test that setDevOptForPurge omits warning and last active when warning timestamp is not positive`() =
        runTest {
            stubSetUserAttributeSuccess()
            val valueCaptor = argumentCaptor<String>()

            underTest.setDevOptForPurge(
                purgeTimestamp = PURGE_TS,
                reason = REASON,
                warningTimestamp = 0L,
                lastActiveTimestamp = LAST_ACTIVE_TS,
            )

            verify(megaApiGateway).setUserAttribute(
                eq(MegaApiJava.USER_ATTR_DEV_OPT),
                valueCaptor.capture(),
                any(),
            )
            val lastPurge = JSONObject(valueCaptor.firstValue).getJSONArray("lastpurge")
            assertThat(lastPurge.length()).isEqualTo(2)
            assertThat(lastPurge.getLong(0)).isEqualTo(PURGE_TS)
            assertThat(lastPurge.getInt(1)).isEqualTo(REASON)
        }

    @Test
    fun `test that setDevOptForPurge omits last active but keeps warning when last active is not positive`() =
        runTest {
            stubSetUserAttributeSuccess()
            val valueCaptor = argumentCaptor<String>()

            underTest.setDevOptForPurge(
                purgeTimestamp = PURGE_TS,
                reason = REASON,
                warningTimestamp = WARNING_TS,
                lastActiveTimestamp = 0L,
            )

            verify(megaApiGateway).setUserAttribute(
                eq(MegaApiJava.USER_ATTR_DEV_OPT),
                valueCaptor.capture(),
                any(),
            )
            val lastPurge = JSONObject(valueCaptor.firstValue).getJSONArray("lastpurge")
            assertThat(lastPurge.length()).isEqualTo(3)
            assertThat(lastPurge.getLong(2)).isEqualTo(WARNING_TS)
        }

    @Test
    fun `test that getLastPurgeAcknowledged returns the request number when the attribute exists`() =
        runTest {
            val request = mock<MegaRequest> { on { number }.thenReturn(ACK_TS) }
            whenever(
                megaApiGateway.getUserAttribute(
                    eq(MegaApiJava.USER_ATTR_LAST_PURGE_ACKNOWLEDGED),
                    any(),
                )
            ).thenAnswer {
                (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), request, megaError(MegaError.API_OK),
                )
            }

            assertThat(underTest.getLastPurgeAcknowledged()).isEqualTo(ACK_TS)
        }

    @Test
    fun `test that getLastPurgeAcknowledged returns zero when the attribute has not been set`() =
        runTest {
            whenever(
                megaApiGateway.getUserAttribute(
                    eq(MegaApiJava.USER_ATTR_LAST_PURGE_ACKNOWLEDGED),
                    any(),
                )
            ).thenAnswer {
                (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), megaError(MegaError.API_ENOENT),
                )
            }

            assertThat(underTest.getLastPurgeAcknowledged()).isEqualTo(0L)
        }

    @Test
    fun `test that setDevOptForPurge throws MegaException when the request fails`() =
        runTest {
            whenever(
                megaApiGateway.setUserAttribute(
                    eq(MegaApiJava.USER_ATTR_DEV_OPT),
                    any<String>(),
                    any(),
                )
            ).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), megaError(MegaError.API_EACCESS),
                )
            }

            val result = runCatching {
                underTest.setDevOptForPurge(
                    purgeTimestamp = PURGE_TS,
                    reason = REASON,
                    warningTimestamp = WARNING_TS,
                    lastActiveTimestamp = LAST_ACTIVE_TS,
                )
            }

            assertThat(result.exceptionOrNull()).isInstanceOf(MegaException::class.java)
        }

    @Test
    fun `test that getLastPurgeAcknowledged throws MegaException when the request fails with an unexpected error`() =
        runTest {
            whenever(
                megaApiGateway.getUserAttribute(
                    eq(MegaApiJava.USER_ATTR_LAST_PURGE_ACKNOWLEDGED),
                    any(),
                )
            ).thenAnswer {
                (it.arguments[1] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(), mock(), megaError(MegaError.API_EACCESS),
                )
            }

            val result = runCatching { underTest.getLastPurgeAcknowledged() }

            assertThat(result.exceptionOrNull()).isInstanceOf(MegaException::class.java)
        }

    private fun stubSetUserAttributeSuccess() {
        whenever(
            megaApiGateway.setUserAttribute(
                eq(MegaApiJava.USER_ATTR_DEV_OPT),
                any<String>(),
                any(),
            )
        ).thenAnswer {
            (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(), mock(), megaError(MegaError.API_OK),
            )
        }
    }

    private fun megaError(code: Int) = mock<MegaError> { on { errorCode }.thenReturn(code) }

    companion object {
        private const val PURGE_TS = 1_000_000L
        private const val WARNING_TS = 900_000L
        private const val LAST_ACTIVE_TS = 500_000L
        private const val REASON = 4
        private const val ACK_TS = 1_234_567L
    }
}
