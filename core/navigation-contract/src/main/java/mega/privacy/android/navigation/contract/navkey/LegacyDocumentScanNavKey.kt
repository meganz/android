package mega.privacy.android.navigation.contract.navkey

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation key that launches the legacy ML Kit document scanner.
 *
 * The legacy scanner is started imperatively via an `ActivityResultLauncher`
 * rather than being a composable destination, so the continuous-scanner router
 * cannot open it directly. This key lets the router (and any entry point) fall
 * back to the legacy scanner uniformly: a single app-shell handler owns the
 * launcher and services this key.
 */
@Serializable
data object LegacyDocumentScanNavKey : NavKey
