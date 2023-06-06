package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import mega.privacy.android.domain.usecase.GetPricing
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetFullAccountInfoUseCaseTest {
    private lateinit var underTest: GetFullAccountInfoUseCase
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase = mock()
    private val getPricing: GetPricing = mock()
    private val getNumberOfSubscription: GetNumberOfSubscription = mock()
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase = mock()
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase = mock()
    private val getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase = mock()

    @Before
    fun setUp() {
        underTest = GetFullAccountInfoUseCase(
            monitorStorageStateEventUseCase = monitorStorageStateEventUseCase,
            getPricing = getPricing,
            getNumberOfSubscription = getNumberOfSubscription,
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            getPaymentMethodUseCase = getPaymentMethodUseCase,
            getSpecificAccountDetailUseCase = getSpecificAccountDetailUseCase,
        )
    }

    @Test
    fun `test that monitorStorageStateEvent return StorageState Unknown then following call`() {
        runTest {
            val event = StorageStateEvent(0L, "", 0L, "", EventType.Storage, StorageState.Unknown)
            whenever(monitorStorageStateEventUseCase()).thenReturn(MutableStateFlow(event))
            underTest()
            verify(getPaymentMethodUseCase).invoke(true)
            verify(getAccountDetailsUseCase).invoke(true)
            verifyNoMoreInteractions(getSpecificAccountDetailUseCase)
            verify(getPricing).invoke(true)
            verify(getNumberOfSubscription).invoke(true)
        }
    }

    @Test
    fun `test that monitorStorageStateEvent return differ StorageState Unknown then following call`() {
        runTest {
            val event = StorageStateEvent(0L, "", 0L, "", EventType.Storage, StorageState.Green)
            whenever(monitorStorageStateEventUseCase()).thenReturn(MutableStateFlow(event))
            underTest()
            verify(getPaymentMethodUseCase).invoke(true)
            verify(getSpecificAccountDetailUseCase).invoke(
                storage = false,
                transfer = true,
                pro = true,
            )
            verifyNoMoreInteractions(getAccountDetailsUseCase)
            verify(getPricing).invoke(true)
            verify(getNumberOfSubscription).invoke(true)
        }
    }
}