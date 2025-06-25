package mega.privacy.android.navigation.contract

import androidx.annotation.DrawableRes
import kotlinx.coroutines.flow.Flow

/**
 * Sealed interface representing different types of navigation drawer items.
 */
sealed interface NavDrawerItem {
    val id: Int
    val destination: Any
    val iconRes: Int
    val title: String

    /**
     * Standard navigation drawer item for account-related features.
     *
     * @property id Unique identifier for the drawer item
     * @property destination Navigation destination
     * @property iconRes Resource ID for the item's icon
     * @property title Display title for the item
     * @property subTitle Optional subtitle displayed below the title
     * @property badge Optional badge text to display on the item
     * @property actionLabel Optional action label for the item
     */
    open class Account(
        override val id: Int,
        override val destination: Any,
        @DrawableRes override val iconRes: Int,
        override val title: String,
        val subTitle: Flow<String?>? = null,
        val badge: Flow<String?>? = null,
        val actionLabel: String? = null,
    ) : NavDrawerItem

    /**
     * Navigation drawer item for privacy suite features.
     *
     * @property id Unique identifier for the drawer item
     * @property destination Navigation destination
     * @property iconRes Resource ID for the item's icon
     * @property title Display title for the item
     * @property subTitle Optional subtitle displayed below the title
     * @property link External link URL for the privacy suite feature
     */
    open class PrivacySuite(
        override val id: Int,
        override val destination: Any,
        @DrawableRes override val iconRes: Int,
        override val title: String,
        val subTitle: String,
        val link: String,
    ) : NavDrawerItem
}

