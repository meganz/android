package mega.privacy.android.app.presentation.myaccount.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color

/**
 * Account Type Attributes data holder
 * @property background as background color resource id
 * @property icon as drawable resource id
 * @property description as string resource id
 */
data class AccountTypeAttributes(
    val background: Color,
    @DrawableRes val icon: Int,
    @StringRes val description: Int
)