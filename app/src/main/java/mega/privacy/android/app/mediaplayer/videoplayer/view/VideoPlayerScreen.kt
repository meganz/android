package mega.privacy.android.app.mediaplayer.videoplayer.view

import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.VideoPlayerPlayerViewBinding
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetLayout

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialNavigationApi::class)
@Composable
internal fun VideoPlayerScreen(
    bottomSheetNavigator: BottomSheetNavigator,
    scaffoldState: ScaffoldState,
    player: ExoPlayer?,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val configuration = LocalConfiguration.current
    val orientation = configuration.orientation

    val systemUiController = rememberSystemUiController()
    var isControllerViewVisible by remember { mutableStateOf(true) }

    val navigationBarHeight = getNavigationBarHeight(orientation, density, layoutDirection)
    val navigationBarHeightPx = with(density) { navigationBarHeight.toPx().toInt() }

    LaunchedEffect(isControllerViewVisible) {
        systemUiController.isSystemBarsVisible = isControllerViewVisible
    }

    MegaBottomSheetLayout(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                testTagsAsResourceId = true
            },
        bottomSheetNavigator = bottomSheetNavigator,
    ) {
        MegaScaffold(
            modifier = Modifier.fillMaxSize(),
            scaffoldState = scaffoldState,
        ) { paddingValues ->
            key(orientation) {
                AndroidViewBinding(
                    modifier = Modifier.fillMaxSize(),
                    factory = VideoPlayerPlayerViewBinding::inflate,
                ) {
                    playerComposeView.player = player

                    val controllerView = root.findViewById<View>(R.id.controls_view)
                    val layoutParams =
                        controllerView.layoutParams as ViewGroup.MarginLayoutParams
                    if (orientation == ORIENTATION_PORTRAIT)
                        layoutParams.bottomMargin = navigationBarHeightPx
                    else
                        layoutParams.marginEnd = navigationBarHeightPx
                    controllerView.layoutParams = layoutParams

                    playerComposeView.setOnClickListener {
                        isControllerViewVisible = !isControllerViewVisible
                        if (isControllerViewVisible) {
                            playerComposeView.showController()
                        } else {
                            playerComposeView.hideController()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getNavigationBarHeight(
    orientation: Int,
    density: Density,
    layoutDirection: LayoutDirection,
): Dp {
    return if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        with(density) { WindowInsets.navigationBars.getRight(density, layoutDirection).toDp() }
    } else {
        with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    }
}