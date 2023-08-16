package mega.privacy.android.app.upgradeAccount.model.extensions

import androidx.compose.runtime.Composable
import mega.privacy.android.app.upgradeAccount.model.UIAccountType
import mega.privacy.android.domain.entity.AccountType

@Composable
internal fun AccountType.toUIAccountType(): UIAccountType {
    return when (this) {
        AccountType.FREE -> UIAccountType.FREE

        AccountType.PRO_LITE -> UIAccountType.PRO_LITE

        AccountType.PRO_I -> UIAccountType.PRO_I

        AccountType.PRO_II -> UIAccountType.PRO_II

        AccountType.PRO_III -> UIAccountType.PRO_III

        else -> UIAccountType.FREE
    }
}