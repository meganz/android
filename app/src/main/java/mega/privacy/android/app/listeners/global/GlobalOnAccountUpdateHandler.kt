package mega.privacy.android.app.listeners.global

import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetNumberOfSubscription
import mega.privacy.android.domain.usecase.GetPricing
import mega.privacy.android.domain.usecase.account.GetUserDataUseCase
import mega.privacy.android.domain.usecase.billing.GetPaymentMethodUseCase
import timber.log.Timber
import javax.inject.Inject

class GlobalOnAccountUpdateHandler @Inject constructor(
    private val dbH: Lazy<DatabaseHandler>,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val getPaymentMethodUseCase: GetPaymentMethodUseCase,
    private val getPricing: GetPricing,
    private val getNumberOfSubscription: GetNumberOfSubscription,
    private val getUserDataUseCase: GetUserDataUseCase,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
) {
    operator fun invoke() {
        Timber.d("onAccountUpdate")

        applicationScope.launch {
            runCatching { getUserDataUseCase() }.onFailure { Timber.e(it) }
            runCatching { getPaymentMethodUseCase(true) }.onFailure { Timber.e(it) }
            runCatching { getPricing(true) }.onFailure { Timber.e(it) }
            runCatching {
                dbH.get().resetExtendedAccountDetailsTimestamp()
            }.onFailure { Timber.e(it) }
            runCatching { getAccountDetailsUseCase(forceRefresh = true) }.onFailure { Timber.e(it) }
            runCatching { getNumberOfSubscription(true) }.onFailure { Timber.e(it) }
        }
    }
}