package mega.privacy.android.navigation.contract

import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

interface MainNavItem {
    val destinationClass: KClass<*>
    val icon: ImageVector
    val label: String
}