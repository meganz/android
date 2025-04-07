package mega.privacy.android.app.presentation.transfers.preview.view.navigation

import androidx.compose.material.ScaffoldState
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

internal const val transferUniqueIdArg = "transferUniqueId"
internal const val fakePreviewNavigationRoutePattern = "fakePreview/{$transferUniqueIdArg}"

internal fun fakePreviewNavigationRoutePattern(transferUniqueId: Long) =
    "fakePreview/$transferUniqueId"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.fakePreviewViewNavigationGraph(
    navHostController: NavHostController,
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
) {
    navigation(
        startDestination = fakePreviewRoute,
        route = fakePreviewNavigationRoutePattern,
    ) {
        fakePreviewScreen(
            navHostController = navHostController,
            scaffoldState = scaffoldState,
            onBackPress = onBackPress,
        )
    }
}

internal fun NavHostController.navigateToFakePreviewViewGraph(
    transferUniqueId: Long,
    navOptions: NavOptions? = null,
) {
    navigate(fakePreviewNavigationRoutePattern(transferUniqueId), navOptions)
}

internal class FakePreviewArgs(val transferUniqueId: Long) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(
                transferUniqueId = checkNotNull(
                    savedStateHandle.get<String>(transferUniqueIdArg)?.toLongOrNull()
                )
            )
}