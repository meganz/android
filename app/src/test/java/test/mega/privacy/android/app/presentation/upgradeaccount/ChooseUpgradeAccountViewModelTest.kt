package test.mega.privacy.android.app.presentation.upgradeaccount

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.app.upgradeAccount.ChooseUpgradeAccountViewModel
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.billing.GetLocalPricingUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class ChooseUpgradeAccountViewModelTest {

    private lateinit var underTest: ChooseUpgradeAccountViewModel

    private val myAccountInfo = mock<MyAccountInfo>()
    private val getPricing = mock<GetPricing>()
    private val getLocalPricingUseCase = mock<GetLocalPricingUseCase>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = ChooseUpgradeAccountViewModel(
            myAccountInfo = myAccountInfo,
            getPricing = getPricing,
            getLocalPricingUseCase = getLocalPricingUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that exception when get pricing is not propagated`() = runTest {
        whenever(getPricing(any())).thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            refreshPricing()
            state.map { it.product }.test {
                assertEquals(awaitItem(), emptyList())
            }
        }
    }
}