package mega.privacy.android.app.presentation.transfers.preview.view.navigation

import androidx.compose.material.ScaffoldState
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi

internal const val transferUniqueIdArg = "transferUniqueId"
internal const val transferTagToCancelArg = "transferTagToCancel"
internal const val fakePreviewNavigationRoutePattern =
    "fakePreview/?transferUniqueIdArg={$transferUniqueIdArg}?transferTagToCancel={$transferTagToCancelArg}"

internal fun fakePreviewNavigationRoutePattern(transferUniqueId: Long?, transferTagToCancel: Int?) =
    "fakePreview/?transferUniqueIdArg=$transferUniqueId?transferTagToCancel=$transferTagToCancel"

@OptIn(ExperimentalMaterialNavigationApi::class)
internal fun NavGraphBuilder.fakePreviewViewNavigationGraph(
    navHostController: NavHostController,
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
    navigateToStorageSettings: () -> Unit,
) {
    navigation(
        startDestination = fakePreviewRoute,
        route = fakePreviewNavigationRoutePattern,
    ) {
        fakePreviewScreen(
            navHostController = navHostController,
            scaffoldState = scaffoldState,
            onBackPress = onBackPress,
            navigateToStorageSettings = navigateToStorageSettings,
        )
    }
}

internal fun NavHostController.navigateToFakePreviewViewGraph(
    transferUniqueId: Long? = null,
    transferTagToCancel: Int? = null,
    navOptions: NavOptions? = null,
) {
    navigate(fakePreviewNavigationRoutePattern(transferUniqueId, transferTagToCancel), navOptions)
}

internal class FakePreviewArgs(val transferUniqueId: Long?, val transferTagToCancel: Int?) {
    constructor(savedStateHandle: SavedStateHandle) :
            this(
                transferUniqueId = savedStateHandle.get<String>(transferUniqueIdArg)
                    ?.toLongOrNull()?.takeUnless { it == -1L },
                transferTagToCancel = savedStateHandle.get<String>(transferTagToCancelArg)
                    ?.toIntOrNull().takeUnless { it == -1 }
            )
}