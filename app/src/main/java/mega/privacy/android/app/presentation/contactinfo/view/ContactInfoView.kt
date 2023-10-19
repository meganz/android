package mega.privacy.android.app.presentation.contactinfo.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ContactInfoView(
    uiState: ContactInfoState,
    onBackPress: () -> Unit,
    statusBarHeight: Float,
    updateNickName: (String?) -> Unit,
    updateNickNameDialogVisibility: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemUiController = rememberSystemUiController()
    systemUiController.setStatusBarColor(
        color = Color.Transparent,
        darkIcons = systemUiController.statusBarDarkContentEnabled
    )
    val scrollState = rememberScrollState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()
    val density = LocalDensity.current.density

    val headerMaxHeight = headerMaxHeight(statusBarHeight)
    val headerMinHeight = headerMinHeight(statusBarHeight)
    val headerGoneHeight = headerGoneHeight(statusBarHeight)
    val headerStartGoneHeight = headerStartGoneHeight(statusBarHeight)

    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = false,
    )

    Scaffold(
        modifier = modifier,
        backgroundColor = Color.Transparent,
        scaffoldState = scaffoldState,
    ) { padding ->

        SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackBarHostState)

        val headerHeight by remember {
            derivedStateOf {
                (headerMaxHeight - (scrollState.value / density)).coerceAtLeast(
                    headerMinHeight
                )
            }
        }
        val progress by remember {
            derivedStateOf {
                1 - ((headerHeight - headerGoneHeight)
                        / (headerStartGoneHeight - headerGoneHeight))
                    .coerceIn(0f, 1f)
            }
        }
        //to set the minimum height of the colum so it's always possible to collapse the header
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            //to set the minimum height of the colum so it's always possible to collapse the header
            val boxWithConstraintsScope = this
            ContactInfoTopAppBar(
                onBackPress = onBackPress,
                avatar = uiState.avatar,
                primaryDisplayName = uiState.primaryDisplayName,
                userChatStatus = uiState.userChatStatus,
                defaultAvatarColor = uiState.contactItem?.defaultAvatarColor,
                progress = progress,
                statusBarHeight = statusBarHeight,
                headerMinHeight = headerMinHeight,
                headerHeight = headerHeight,
            )
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(top = headerMaxHeight.dp)
                    .fillMaxSize()
            ) {
                ContactInfoContent(
                    uiState = uiState,
                    modifier = Modifier.heightIn(boxWithConstraintsScope.maxHeight - headerMinHeight.dp),
                    coroutineScope = coroutineScope,
                    modalSheetState = modalSheetState,
                    updateNickNameDialogVisibility = updateNickNameDialogVisibility
                )
            }
        }
    }

    BackHandler(enabled = modalSheetState.isVisible) {
        coroutineScope.launch {
            modalSheetState.hide()
        }
    }

    ContactInfoBottomSheet(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        updateNickName = updateNickName,
        updateNickNameDialogVisibility = updateNickNameDialogVisibility
    )

    if (uiState.showUpdateAliasDialog) {
        UpdateNickNameDialog(
            hasAlias = uiState.hasAlias,
            updateNickName = updateNickName,
            updateNickNameDialogVisibility = updateNickNameDialogVisibility,
            nickName = uiState.contactItem?.contactData?.alias
        )
    }
}

private const val appBarHeight = 56f
private fun headerMinHeight(statusBarHeight: Float) = appBarHeight + statusBarHeight
private fun headerMaxHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 76f
private fun headerStartGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 56f
private fun headerGoneHeight(statusBarHeight: Float) = headerMinHeight(statusBarHeight) + 18f

@Preview
@Composable
private fun ContactInfoPreview() {
    val contactData = ContactData(
        alias = "Iron Man",
        avatarUri = "https://avatar.uri.com",
        fullName = "Tony Stark",
    )
    ContactInfoView(
        uiState = ContactInfoState(
            contactItem = ContactItem(
                handle = 123456L,
                email = "test@gmail.com",
                contactData = contactData,
                defaultAvatarColor = "red",
                visibility = UserVisibility.Visible,
                timestamp = 123456789,
                areCredentialsVerified = true,
                status = UserChatStatus.Online,
                lastSeen = 0,
            ),
        ),
        onBackPress = {},
        statusBarHeight = 12f,
        updateNickName = { },
        updateNickNameDialogVisibility = {},
    )
}