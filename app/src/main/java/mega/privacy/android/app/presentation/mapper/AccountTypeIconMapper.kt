package mega.privacy.android.app.presentation.mapper

import androidx.compose.ui.graphics.vector.ImageVector
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.icon.pack.IconPack
import javax.inject.Inject

/**
 * Mapper to get the icon for a specific account type
 */
class AccountTypeIconMapper @Inject constructor() {

    /**
     * Maps account type to its corresponding shield icon
     *
     * @param accountType The account type to map
     * @return The corresponding shield icon
     */
    operator fun invoke(
        accountType: AccountType?
    ): ImageVector = when (accountType) {
        AccountType.PRO_LITE -> IconPack.Medium.Thin.Outline.ShieldLite
        AccountType.PRO_I -> IconPack.Medium.Thin.Outline.Shield01
        AccountType.PRO_II -> IconPack.Medium.Thin.Outline.Shield02
        AccountType.PRO_III -> IconPack.Medium.Thin.Outline.Shield03
        else -> IconPack.Medium.Thin.Outline.Shield
    }
}
