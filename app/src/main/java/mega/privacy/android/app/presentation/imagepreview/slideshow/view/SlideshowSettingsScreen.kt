package mega.privacy.android.app.presentation.imagepreview.slideshow.view

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.slideshow.SlideshowSettingViewModel
import mega.privacy.android.app.presentation.slideshow.view.SlideshowSettingsView
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar

@Composable
fun SlideshowSettingScreen(
    slideshowSettingViewModel: SlideshowSettingViewModel = hiltViewModel(),
) {
    val onBackPressedDispatcher =
        LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        scaffoldState = rememberScaffoldState(),
        topBar = {
            MegaAppBar(
                title = stringResource(R.string.slideshow_settings_page_title),
                appBarType = AppBarType.BACK_NAVIGATION,
                elevation = 0.dp,
                onNavigationPressed = {
                    onBackPressedDispatcher?.onBackPressed()
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            SlideshowSettingsView(
                slideshowSettingViewModel = slideshowSettingViewModel,
                topPadding = 0.dp,
            )
        }
    }
}