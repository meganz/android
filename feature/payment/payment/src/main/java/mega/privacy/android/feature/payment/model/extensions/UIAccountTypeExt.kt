package mega.privacy.android.feature.payment.model.extensions

import androidx.compose.runtime.Composable
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.feature.payment.model.UIAccountType

/**
 * Extension function to convert AccountType to UIAccountType.
 * Starter, Basic and Essential AccountType should not be mapped to any UIAccountType.
 * @return Converted UIAccountType.
 */
@Composable
fun AccountType.toUIAccountType(): UIAccountType {
    return when (this) {
        AccountType.FREE -> UIAccountType.FREE

        AccountType.PRO_LITE -> UIAccountType.PRO_LITE

        AccountType.PRO_I -> UIAccountType.PRO_I

        AccountType.PRO_II -> UIAccountType.PRO_II

        AccountType.PRO_III -> UIAccountType.PRO_III

        else -> UIAccountType.FREE
    }
}

/**
 * Extension function to convert AccountType to web client product ID.
 *
 * This function maps internal account types to their corresponding product IDs
 * used by the web client for external purchases. These product IDs are used
 * in the external checkout URL generation.
 *
 * @return The product ID string for the account type, or empty string if the account type
 *         is not supported for external checkout
 */
internal fun AccountType.toWebClientProductId(): String = when (this) {
    AccountType.PRO_LITE -> "propay_101"
    AccountType.PRO_I -> "propay_1"
    AccountType.PRO_II -> "propay_2"
    AccountType.PRO_III -> "propay_3"
    AccountType.BUSINESS -> "registerb"
    AccountType.PRO_FLEXI -> "propay_4"
    else -> ""
}
