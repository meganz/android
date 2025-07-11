package mega.privacy.android.navigation.contract

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.Flow

/**
 * Sealed interface representing different types of navigation drawer items.
 */
sealed interface NavDrawerItem {
    val destination: Any
    val icon: ImageVector
    val title: Int

    /**
     * Standard navigation drawer item for account-related features.
     *
     * @property destination Navigation destination
     * @property icon the item's icon
     * @property title Display title for the item
     * @property subTitle Optional subtitle displayed below the title
     * @property badge Optional badge text to display on the item
     * @property actionLabel Optional action label for the item
     */
    open class Account(
        override val destination: Any,
        override val icon: ImageVector,
        @StringRes override val title: Int,
        val subTitle: Flow<String?>? = null,
        val badge: Flow<String?>? = null,
        @StringRes val actionLabel: Int? = null,
    ) : NavDrawerItem

    /**
     * Navigation drawer item for privacy suite features.
     *
     * @property destination Navigation destination
     * @property icon the item's icon
     * @property title Display title for the item
     * @property subTitle Optional subtitle displayed below the title
     * @property link External link URL for the privacy suite feature
     */
    open class PrivacySuite(
        override val destination: Any,
        override val icon: ImageVector,
        @StringRes override val title: Int,
        @StringRes val subTitle: Int,
        val link: String,
        val appPackage: String? = null
    ) : NavDrawerItem
}

