package mega.privacy.android.domain.usecase.billing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.domain.entity.billing.RecommendedSubscriptionOffer
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor the subscription offer to promote, re-evaluating it whenever the account plan changes.
 *
 * The API clears the campaign flag on the plans it no longer targets once one of them is bought, so
 * re-running [GetRecommendedSubscriptionWithOfferUseCase] on every plan change is what makes the
 * offer disappear by itself from the Home banner, the menu banner and the offer landing screen
 * after an upgrade.
 *
 * The lookup behind it is expensive — an SDK pricing request plus a Play Billing product query,
 * neither of which is cached downstream — and several surfaces promote the same offer. So the
 * result is shared application-wide: the last offer is replayed to a new subscriber immediately,
 * letting the menu banner render from what the Home banner already resolved instead of repeating
 * the lookup on every tab switch. The upstream is kept alive briefly after the last subscriber
 * leaves so a tab switch does not restart it at all, and it re-runs on the next subscription to
 * pick up a campaign that started meanwhile.
 *
 * The lookup runs on the first emission rather than waiting for the account details to arrive,
 * which is what keeps the banner quick; the promoted plan does not depend on the account plan
 * anyway, only on which plans the campaign discounts.
 *
 * No offer is promoted while the device is offline: the offer cannot be bought without a
 * connection, and without this gate the replay cache would keep showing an offer resolved earlier
 * in the session. The lookup runs again as soon as connectivity is back, so the offer returns on
 * its own.
 *
 * A failed lookup is emitted as a failed [Result] instead of terminating the flow, so a transient
 * billing error still leaves the offer monitored; consumers with nowhere to surface an error treat
 * it as "no offer".
 *
 * @property monitorAccountDetailUseCase                [MonitorAccountDetailUseCase]
 * @property monitorConnectivityUseCase                 [MonitorConnectivityUseCase]
 * @property getRecommendedSubscriptionWithOfferUseCase [GetRecommendedSubscriptionWithOfferUseCase]
 * @property scope                                      the application scope the offer is shared in
 */
@Singleton
class MonitorSubscriptionOfferUseCase @Inject constructor(
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val getRecommendedSubscriptionWithOfferUseCase: GetRecommendedSubscriptionWithOfferUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val offer: Flow<Result<RecommendedSubscriptionOffer?>> by lazy {
        combine(
            monitorAccountDetailUseCase().map { it.levelDetail?.accountType },
            monitorConnectivityUseCase(),
            ::Pair,
        ).distinctUntilChanged()
            .mapLatest { (_, isConnected) ->
                if (isConnected) {
                    runCatching { getRecommendedSubscriptionWithOfferUseCase() }
                        .onFailure { if (it is CancellationException) throw it }
                } else {
                    Result.success(null)
                }
            }
            .shareIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                replay = 1,
            )
    }

    /**
     * Invoke
     *
     * @return [Flow] emitting the offer to promote, null when no plan carries one
     */
    operator fun invoke(): Flow<Result<RecommendedSubscriptionOffer?>> = offer
}

/**
 * How long the shared lookup stays alive after the last subscriber leaves, long enough to cover a
 * tab switch between two surfaces that both promote the offer.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L
