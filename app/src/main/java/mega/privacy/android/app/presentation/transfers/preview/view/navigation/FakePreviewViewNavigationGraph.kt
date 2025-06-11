package mega.privacy.android.app.presentation.transfers.preview.view.navigation

import androidx.compose.material.ScaffoldState
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.navigation

internal const val transferPathArg = "transferPath"
internal const val transferUniqueIdArg = "transferUniqueId"
internal const val transferTagToCancelArg = "transferTagToCancel"
internal const val fakePreviewNavigationRoutePattern =
    "fakePreview/?transferPathArg={$transferPathArg}" +
            "?transferUniqueIdArg={$transferUniqueIdArg}" +
            "?transferTagToCancel={$transferTagToCancelArg}"

internal fun fakePreviewNavigationRoutePattern(
    transferPath: String?,
    transferUniqueId: Long?,
    transferTagToCancel: Int?,
) = "fakePreview/?transferPathArg=$transferPath" +
        "?transferUniqueIdArg=$transferUniqueId" +
        "?transferTagToCancel=$transferTagToCancel"

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
    transferPath: String? = null,
    transferUniqueId: Long? = null,
    transferTagToCancel: Int? = null,
    navOptions: NavOptions? = null,
) {
    navigate(
        fakePreviewNavigationRoutePattern(transferPath, transferUniqueId, transferTagToCancel),
        navOptions
    )
}

internal class FakePreviewArgs(
    val transferPath: String?,
    val transferUniqueId: Long?,
    val transferTagToCancel: Int?,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        transferPath = savedStateHandle.get<String>(transferPathArg),
        transferUniqueId = savedStateHandle.get<String>(transferUniqueIdArg)
            ?.toLongOrNull()?.takeUnless { it == -1L },
        transferTagToCancel = savedStateHandle.get<String>(transferTagToCancelArg)
            ?.toIntOrNull().takeUnless { it == -1 }
    )
}