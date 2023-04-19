package mega.privacy.android.app.presentation.myaccount.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Account Type Attributes data holder
 * @property background as background color resource id
 * @property icon as drawable resource id
 * @property description as string resource id
 */
data class AccountTypeAttributes(
    @ColorRes val background: Int,
    @DrawableRes val icon: Int,
    @StringRes val description: Int
)