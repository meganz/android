package mega.privacy.android.app.presentation.myaccount.widget

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetViewHolder
import mega.privacy.android.navigation.destination.MyAccountNavKey
import javax.inject.Inject

class SingleCardExampleHomeWidget @Inject constructor(
) : HomeWidget {
    override val identifier: String = "AccountHomeWidgetProvider"
    override val defaultOrder: Int = 0
    override val canDelete: Boolean = false

    override suspend fun getWidgetName() = LocalizedText.Literal("My Account")

    override fun getWidget(): Flow<HomeWidgetViewHolder> {
        var uiState = MyAccountHomeUIState(
            name = "John Doe",
            accountType = null,
            accountDetail = AccountDetail(
                storageDetail = AccountStorageDetail(
                    usedStorage = 100L,
                    totalStorage = 1_000,
                    usedCloudDrive = 0L,
                    usedRubbish = 0L,
                    usedIncoming = 0L,
                    subscriptionMethodId = 0,
                ),
            ),
            accountTypeNameResource = R.string.pro2_account,
        )
        return flow {
            emit(
                uiState
            )
            repeat(9) {
                delay(1000)
                val accountDetails = uiState.accountDetail
                uiState = uiState.copy(
                    accountDetail = accountDetails?.copy(
                        storageDetail = accountDetails.storageDetail?.copy(
                            usedStorage = accountDetails.storageDetail?.usedStorage?.plus(
                                100L
                            ) ?: 0L
                        )
                    )
                )
                emit(uiState)
            }
        }.map { state ->
            HomeWidgetViewHolder(
                widgetFunction = { modifier, onNavigate ->
                    MyAccountHomeWidget(state, modifier, { onNavigate(MyAccountNavKey)})
                },
            )
        }
    }

}