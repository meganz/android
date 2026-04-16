package mega.privacy.android.shared.ads.adsfreeintro

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.formatter.mapper.FormattedPriceMapper
import mega.privacy.android.core.formatter.mapper.FormattedSizeMapper
import mega.privacy.android.core.formatter.model.FormattedSize
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.billing.GetRecommendedSubscriptionUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdsFreeIntroViewModelTest {

    private lateinit var viewModel: AdsFreeIntroViewModel
    private val getRecommendedSubscriptionUseCase: GetRecommendedSubscriptionUseCase = mock()
    private val formattedPriceMapper: FormattedPriceMapper = mock()
    private val formattedSizeMapper: FormattedSizeMapper = mock()
    private val monitorThemeModeUseCase = mock<MonitorThemeModeUseCase> {
        on { invoke() }.thenReturn(
            flowOf(ThemeMode.System)
        )
    }

    @BeforeEach
    fun reset() {
        reset(
            getRecommendedSubscriptionUseCase,
            formattedPriceMapper,
            formattedSizeMapper
        )
    }

    private fun initViewModel() {
        viewModel =
            AdsFreeIntroViewModel(
                getRecommendedSubscriptionUseCase,
                formattedPriceMapper,
                formattedSizeMapper,
                monitorThemeModeUseCase
            )
    }

    @Test
    fun `test that formattedPrice and storageSize are updated on success`() = runTest {
        val expectedFormattedSize = FormattedSize(unit = 0, size = "400")
        val expectedPrice = "€4.99"
        val amount = CurrencyAmount(4.99f, Currency("EUR"))
        val subscription = mock<Subscription> {
            on { this.amount }.thenReturn(amount)
            on { storage }.thenReturn(400)
        }
        whenever(getRecommendedSubscriptionUseCase()).thenReturn(subscription)
        whenever(formattedPriceMapper(amount)).thenReturn(expectedPrice)
        whenever(formattedSizeMapper(400, false)).thenReturn(expectedFormattedSize)
        initViewModel()

        viewModel.state.test {
            val item = awaitItem()
            assertThat(item.formattedPrice).isEqualTo(expectedPrice)
            assertThat(item.storageSize).isEqualTo(expectedFormattedSize)
        }
    }

    @Test
    fun `test that formattedPrice and storageSize are null on failure`() = runTest {
        whenever(getRecommendedSubscriptionUseCase()).thenThrow(RuntimeException("Error"))
        initViewModel()
        viewModel.state.test {
            val item = awaitItem()
            assertThat(item.formattedPrice).isNull()
            assertThat(item.storageSize).isNull()
        }
    }
}
