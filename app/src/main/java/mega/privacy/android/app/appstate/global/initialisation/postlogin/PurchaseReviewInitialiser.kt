package mega.privacy.android.app.appstate.global.initialisation.postlogin

import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.usecase.billing.MonitorSuccessfulPurchasesUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.PostLoginInitialiserAction
import javax.inject.Inject

/**
 * Purchase review initialiser
 * Updates the purchase flag for the review handler if a successful purchase is made
 *
 * @param monitorSuccessfulPurchasesUseCase
 * @param ratingHandlerImpl
 */
class PurchaseReviewInitialiser @Inject constructor(
    monitorSuccessfulPurchasesUseCase: MonitorSuccessfulPurchasesUseCase,
    ratingHandlerImpl: RatingHandlerImpl,
) : PostLoginInitialiserAction(
    action = { _, _ ->
        monitorSuccessfulPurchasesUseCase()
            .collect {
                ratingHandlerImpl.updateTransactionFlag(true)
            }
    }
)