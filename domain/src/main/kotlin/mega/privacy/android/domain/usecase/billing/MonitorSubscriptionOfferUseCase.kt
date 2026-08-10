package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import javax.inject.Inject

/**
 * Monitor the subscription offer to promote, re-evaluating it whenever the account plan changes.
 *
 * The API clears the campaign flag on the plans it no longer targets once one of them is bought, so
 * re-running [GetRecommendedSubscriptionWithOfferUseCase] on every plan change is what makes the
 * offer disappear by itself from the Home banner, the menu banner and the offer landing screen
 * after an upgrade.
 *
 * A failed lookup is emitted as a failed [Result] instead of terminating the flow, so a transient
 * billing error still leaves the offer monitored; consumers with nowhere to surface an error treat
 * it as "no offer".
 *
 * @property monitorAccountDetailUseCase                [MonitorAccountDetailUseCase]
 * @property getRecommendedSubscriptionWithOfferUseCase [GetRecommendedSubscriptionWithOfferUseCase]
 */
class MonitorSubscriptionOfferUseCase @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getRecommendedSubscriptionWithOfferUseCase: GetRecommendedSubscriptionWithOfferUseCase,
) {
    /**
     * Invoke
     *
     * @return [Flow] emitting the offer to promote, null when no plan carries one
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Result<RecommendedSubscriptionOffer?>> =
        monitorAccountDetailUseCase()
            .map { it.levelDetail?.accountType }
            .distinctUntilChanged()
            .mapLatest {
                runCatching { getRecommendedSubscriptionWithOfferUseCase() }
                    .onFailure { if (it is CancellationException) throw it }
            }
}
