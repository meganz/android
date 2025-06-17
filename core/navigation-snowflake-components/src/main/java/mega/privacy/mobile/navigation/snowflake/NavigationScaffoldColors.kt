package mega.privacy.mobile.navigation.snowflake

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItemColors
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import mega.android.core.ui.tokens.theme.DSTokens

internal object NavigationScaffoldColors {
    @Composable
    fun scaffoldColors() = NavigationSuiteDefaults.colors(
        navigationBarContainerColor = DSTokens.colors.background.pageBackground,
        navigationRailContainerColor = DSTokens.colors.background.pageBackground,
        navigationDrawerContainerColor = DSTokens.colors.background.pageBackground,
    )

    @Composable
    fun itemColors() = NavigationSuiteItemColors(
        navigationBarItemColors = NavigationBarItemColors(
            selectedIconColor = DSTokens.colors.icon.brand,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedIndicatorColor = Color.Transparent, // Transparent background
            selectedTextColor = DSTokens.colors.icon.brand,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationRailItemColors = NavigationRailItemColors(
            selectedIconColor = DSTokens.colors.icon.brand,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedIndicatorColor = Color.Transparent, // Transparent background
            selectedTextColor = DSTokens.colors.icon.brand,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedContainerColor = MaterialTheme.colorScheme.surface,
            selectedIconColor = DSTokens.colors.icon.brand,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedTextColor = DSTokens.colors.icon.brand,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedBadgeColor = MaterialTheme.colorScheme.primary,
            unselectedBadgeColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}