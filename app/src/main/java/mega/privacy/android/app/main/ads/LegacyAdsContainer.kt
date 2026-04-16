package mega.privacy.android.app.main.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import mega.privacy.android.shared.ads.AdsContainer
import mega.privacy.android.shared.ads.AdsContainerViewModel
import mega.privacy.android.shared.ads.adsfreeintro.AdsFreeIntroView

/**
 * Legacy ads container that shows AdsFreeIntroView dialog when close button is tapped.
 * Used by legacy callers (ManagerActivity, FileLinkView, FolderLinkView).
 */
@Composable
fun LegacyAdsContainer(
    request: BannerAdRequest?,
    modifier: Modifier = Modifier,
    isLoggedInUser: Boolean = true,
    viewModel: AdsContainerViewModel = hiltViewModel(),
) {
    var showAdsFreeDialog by rememberSaveable { mutableStateOf(false) }

    AdsContainer(
        request = request,
        modifier = modifier,
        isLoggedInUser = isLoggedInUser,
        viewModel = viewModel,
        onCloseAds = { showAdsFreeDialog = true },
    )

    if (showAdsFreeDialog) {
        AdsFreeIntroView(onDismiss = { showAdsFreeDialog = false })
    }
}
