package mega.privacy.android.data.repository.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.account.business.BusinessAccountStatusMapper
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.repository.BusinessRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BusinessRepositoryImplTest {
    private lateinit var underTest: BusinessRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val appEventGateway = mock<AppEventGateway>()
    private val businessAccountStatusMapper = mock<BusinessAccountStatusMapper>()

    @BeforeAll
    fun setUp() {
        underTest = BusinessRepositoryImpl(
            megaApiGateway = megaApiGateway,
            businessAccountStatusMapper = businessAccountStatusMapper,
            appEventGateway = appEventGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            businessAccountStatusMapper,
            appEventGateway,
        )
    }

    @Test
    fun `test that get business status returns api mapped status`() = runTest {
        val status = 1
        val businessAccountStatus = mock<BusinessAccountStatus>()
        whenever(megaApiGateway.getBusinessStatus()).thenReturn(status)
        whenever(businessAccountStatusMapper.invoke(status)).thenReturn(businessAccountStatus)
        assertThat(underTest.getBusinessStatus()).isEqualTo(businessAccountStatus)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that is business account active returns api result`(
        expected: Boolean,
    ) = runTest {
        whenever(megaApiGateway.isBusinessAccountActive()).thenReturn(expected)
        assertThat(underTest.isBusinessAccountActive()).isEqualTo(expected)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that is master business account returns api result`(
        expected: Boolean,
    ) = runTest {
        whenever(megaApiGateway.isMasterBusinessAccount()).thenReturn(expected)
        assertThat(underTest.isMasterBusinessAccount()).isEqualTo(expected)
    }

    @Test
    fun `test that broadcast business account expired emits the event into the gateway`() =
        runTest {
            underTest.broadcastBusinessAccountExpired()
            verify(appEventGateway).broadcastBusinessAccountExpired()
        }

    @Test
    fun `test that monitor business account expired returns gateway flow`() {
        val flow = mock<Flow<Unit>>()
        whenever(appEventGateway.monitorBusinessAccountExpired()).thenReturn(flow)
        assertThat(underTest.monitorBusinessAccountExpired()).isEqualTo(flow)
    }
}