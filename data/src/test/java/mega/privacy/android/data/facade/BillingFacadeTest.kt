package mega.privacy.android.data.facade

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryPurchasesParams
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.VerifyPurchaseGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.mapper.MegaPurchaseMapper
import mega.privacy.android.data.mapper.MegaSkuMapper
import mega.privacy.android.domain.entity.billing.MegaPurchase
import mega.privacy.android.domain.exception.ConnectBillingServiceException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BillingFacadeTest {

    private lateinit var underTest: BillingFacade

    private val context = mock<Context>()
    private val megaSkuMapper = mock<MegaSkuMapper>()
    private val megaPurchaseMapper = mock<MegaPurchaseMapper>()
    private val verifyPurchaseGateway = mock<VerifyPurchaseGateway>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val appEventGateway = mock<AppEventGateway>()
    private val activeSubscription = mock<Cache<MegaPurchase>>()

    private val builder = mock<BillingClient.Builder>(defaultAnswer = Mockito.RETURNS_SELF)
    private lateinit var billingClientStatic: MockedStatic<BillingClient>

    private val okResult = billingResult(BillingClient.BillingResponseCode.OK)
    private val errorResult = billingResult(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)

    @BeforeEach
    fun setUp() {
        Mockito.reset(builder, activeSubscription, appEventGateway)
        billingClientStatic = mockStatic(BillingClient::class.java)
        billingClientStatic.`when`<BillingClient.Builder> { BillingClient.newBuilder(any()) }
            .thenReturn(builder)
    }

    @AfterEach
    fun tearDown() {
        billingClientStatic.close()
    }

    private fun initUnderTest(fetchNodesFinish: Flow<Boolean> = emptyFlow()) {
        whenever(appEventGateway.monitorFetchNodesFinish()).thenReturn(fetchNodesFinish)
        val dispatcher = UnconfinedTestDispatcher()
        underTest = BillingFacade(
            context = context,
            megaSkuMapper = megaSkuMapper,
            megaPurchaseMapper = megaPurchaseMapper,
            ioDispatcher = dispatcher,
            // Its own scheduler is never advanced, so the ProcessLifecycleOwner registration in init does not run.
            mainDispatcher = StandardTestDispatcher(TestCoroutineScheduler()),
            verifyPurchaseGateway = verifyPurchaseGateway,
            applicationScope = CoroutineScope(dispatcher),
            skusCache = mock(),
            megaApiGateway = megaApiGateway,
            productDetailsListCache = mock(),
            activeSubscription = activeSubscription,
            source = mock(),
            appEventGateway = appEventGateway,
        )
    }

    private fun billingResult(responseCode: Int) = mock<BillingResult> {
        on { this.responseCode } doReturn responseCode
        on { debugMessage } doReturn ""
    }

    private fun billingClient(setupResult: BillingResult = okResult) = mock<BillingClient> {
        on { startConnection(any()) } doAnswer {
            it.getArgument<BillingClientStateListener>(0).onBillingSetupFinished(setupResult)
        }
        on { isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS) } doReturn okResult
    }

    private fun stubQueryPurchases(client: BillingClient, vararg results: BillingResult) {
        val remaining = ArrayDeque(results.toList())
        doAnswer {
            it.getArgument<PurchasesResponseListener>(1)
                .onQueryPurchasesResponse(remaining.removeFirst(), emptyList<Purchase>())
        }.whenever(client).queryPurchasesAsync(any<QueryPurchasesParams>(), any())
    }

    @Test
    fun `test that queryPurchase does not update the active subscription when the subscription query fails`() =
        runTest {
            val client = billingClient()
            whenever(builder.build()).thenReturn(client)
            stubQueryPurchases(client, okResult, errorResult)
            initUnderTest()

            val actual = underTest.queryPurchase()

            assertThat(actual).isEmpty()
            verify(activeSubscription, never()).set(any())
        }

    @Test
    fun `test that queryPurchase updates the active subscription when both queries succeed`() =
        runTest {
            val client = billingClient()
            whenever(builder.build()).thenReturn(client)
            stubQueryPurchases(client, okResult, okResult)
            initUnderTest()

            underTest.queryPurchase()

            verify(activeSubscription).set(null)
        }

    @Test
    fun `test that ensureConnect ends the new client when setup fails`() = runTest {
        val client = billingClient(setupResult = errorResult)
        whenever(builder.build()).thenReturn(client)
        initUnderTest()

        assertThrows<ConnectBillingServiceException> {
            underTest.isSubscriptionFeatureAvailable()
        }
        verify(client).endConnection()
    }

    @Test
    fun `test that ensureConnect creates a new client on the next call when setup fails`() =
        runTest {
            val failedClient = billingClient(setupResult = errorResult)
            val newClient = billingClient()
            whenever(builder.build()).thenReturn(failedClient, newClient)
            initUnderTest()

            runCatching { underTest.isSubscriptionFeatureAvailable() }
            val actual = underTest.isSubscriptionFeatureAvailable()

            assertThat(actual).isTrue()
            verify(newClient).startConnection(any())
        }

    @Test
    fun `test that ensureConnect ends the previous client when it is not ready`() = runTest {
        val previousClient = billingClient()
        val newClient = billingClient()
        whenever(builder.build()).thenReturn(previousClient, newClient)
        initUnderTest()

        underTest.isSubscriptionFeatureAvailable()
        whenever(previousClient.isReady).thenReturn(false)
        underTest.isSubscriptionFeatureAvailable()

        verify(previousClient).endConnection()
        verify(newClient).startConnection(any())
    }

    @Test
    fun `test that ensureConnect reuses the client when it is ready`() = runTest {
        val client = billingClient()
        whenever(builder.build()).thenReturn(client)
        initUnderTest()

        underTest.isSubscriptionFeatureAvailable()
        whenever(client.isReady).thenReturn(true)
        underTest.isSubscriptionFeatureAvailable()

        verify(builder, times(1)).build()
        verify(client, never()).endConnection()
    }

    @Test
    fun `test that init keeps querying purchases on fetch nodes finish when a query fails`() =
        runTest {
            val client = billingClient(setupResult = errorResult)
            whenever(builder.build()).thenReturn(client)

            initUnderTest(fetchNodesFinish = flowOf(true, true))

            verify(client, times(2)).startConnection(any())
        }
}
