package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.usecase.impl.DefaultGetFullAccountInfo
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetFullAccountInfoTest {
    private lateinit var underTest: DefaultGetFullAccountInfo
    private val monitorStorageStateEvent: MonitorStorageStateEvent = mock()
    private val getPricing: GetPricing = mock()
    private val getNumberOfSubscription: GetNumberOfSubscription = mock()
    private val getAccountDetails: GetAccountDetails = mock()
    private val getPaymentMethod: GetPaymentMethod = mock()
    private val getSpecificAccountDetail: GetSpecificAccountDetail = mock()

    @Before
    fun setUp() {
        underTest = DefaultGetFullAccountInfo(
            monitorStorageStateEvent = monitorStorageStateEvent,
            getPricing = getPricing,
            getNumberOfSubscription = getNumberOfSubscription,
            getAccountDetails = getAccountDetails,
            getPaymentMethod = getPaymentMethod,
            getSpecificAccountDetail = getSpecificAccountDetail,
        )
    }

    @Test
    fun `test that monitorStorageStateEvent return StorageState Unknown then following call`() {
        runTest {
            val event = StorageStateEvent(0L, "", 0L, "", EventType.Storage, StorageState.Unknown)
            whenever(monitorStorageStateEvent()).thenReturn(MutableStateFlow(event))
            underTest()
            verify(getPaymentMethod, times(1)).invoke(true)
            verify(getAccountDetails, times(1)).invoke(true)
            verifyNoMoreInteractions(getSpecificAccountDetail)
            verify(getPricing, times(1)).invoke(true)
            verify(getNumberOfSubscription, times(1)).invoke(true)
        }
    }

    @Test
    fun `test that monitorStorageStateEvent return differ StorageState Unknown then following call`() {
        runTest {
            val event = StorageStateEvent(0L, "", 0L, "", EventType.Storage, StorageState.Green)
            whenever(monitorStorageStateEvent()).thenReturn(MutableStateFlow(event))
            underTest()
            verify(getPaymentMethod, times(1)).invoke(true)
            verify(getSpecificAccountDetail, times(1)).invoke(
                storage = false,
                transfer = true,
                pro = true,
            )
            verifyNoMoreInteractions(getAccountDetails)
            verify(getPricing, times(1)).invoke(true)
            verify(getNumberOfSubscription, times(1)).invoke(true)
        }
    }
}