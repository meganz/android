package mega.privacy.android.app.menu.presentation

import androidx.compose.ui.graphics.Color
import mega.privacy.android.navigation.contract.NavDrawerItem
import java.io.File

data class MenuUiState(
    val myAccountItems: Map<Int, NavDrawerItem.Account> = emptyMap(),
    val privacySuiteItems: Map<Int, NavDrawerItem.PrivacySuite> = emptyMap(),
    val name: String? = null,
    val email: String? = null,
    val avatar: File? = null,
    val avatarColor: Color = Color.Unspecified,
    val isConnectedToNetwork: Boolean = true,
)
