package mega.privacy.android.app.main.ads

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedSubscriptionMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Subscription
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
    private val localisedSubscriptionMapper: LocalisedSubscriptionMapper = mock()

    @BeforeEach
    fun reset() {
        reset(
            getRecommendedSubscriptionUseCase,
            localisedSubscriptionMapper
        )
    }

    private fun initViewModel() {
        viewModel =
            AdsFreeIntroViewModel(getRecommendedSubscriptionUseCase, localisedSubscriptionMapper)
    }

    @Test
    fun `cheapest subscription is updated on success`() = runTest {
        val subscription = mock<Subscription>()
        val localisedSubscription = mock<LocalisedSubscription>()
        whenever(getRecommendedSubscriptionUseCase()).thenReturn(subscription)
        whenever(localisedSubscriptionMapper(subscription, subscription)).thenReturn(
            localisedSubscription
        )
        initViewModel()

        viewModel.state.test {
            val item = awaitItem()
            assertThat(item.cheapestSubscriptionAvailable).isEqualTo(localisedSubscription)
        }
    }

    @Test
    fun `cheapest subscription is null on failure`() = runTest {
        val exception = RuntimeException("Error")
        whenever(getRecommendedSubscriptionUseCase()).thenThrow(exception)
        initViewModel()
        viewModel.state.test {
            val item = awaitItem()
            assertThat(item.cheapestSubscriptionAvailable).isNull()
        }
    }
}