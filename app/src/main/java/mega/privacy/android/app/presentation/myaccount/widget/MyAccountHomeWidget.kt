package mega.privacy.android.app.presentation.myaccount.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.indicators.ProgressBarIndicator
import mega.android.core.ui.components.surface.CardSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.myaccount.model.MyAccountHomeUIState
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountStorageDetail

@Composable
internal fun MyAccountHomeWidget(state: MyAccountHomeUIState, modifier: Modifier = Modifier) {
    CardSurface(surfaceColor = SurfaceColor.Surface1) {
        Column(modifier = modifier.padding(8.dp)) {
            MegaText(text = state.name ?: "")
            MegaText(stringResource(state.accountTypeNameResource))
            MegaText("${state.accountDetail?.storageDetail?.usedStorage} / ${state.accountDetail?.storageDetail?.totalStorage}")
            ProgressBarIndicator(
                progressPercentage = state.accountDetail?.storageDetail?.usedPercentage?.toFloat()
                    ?: 0F
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewMyAccountHomeWidget() {
    AndroidThemeForPreviews {
        MyAccountHomeWidget(
            state = MyAccountHomeUIState(
                name = "John Doe",
                accountType = null,
                accountDetail = AccountDetail(
                    storageDetail = AccountStorageDetail(
                        usedStorage = 500L,
                        totalStorage = 1_000,
                        usedCloudDrive = 0L,
                        usedRubbish = 0L,
                        usedIncoming = 0L,
                        subscriptionMethodId = 0,
                    ),
                ),
                accountTypeNameResource = R.string.pro2_account,
            ),
        )
    }
}