package mega.privacy.android.app.appstate.initialisation.postlogin

import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.service.iar.RatingHandlerImpl
import mega.privacy.android.domain.usecase.billing.MonitorSuccessfulPurchasesUseCase
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
) : PostLoginInitialiser(
    action = { _ ->
        monitorSuccessfulPurchasesUseCase()
            .collect {
                ratingHandlerImpl.updateTransactionFlag(true)
            }
    }
)