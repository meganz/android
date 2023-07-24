package mega.privacy.android.app.presentation.contactinfo.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.entity.user.UserVisibility


@Composable
internal fun ContactInfoView(
    uiState: ContactInfoState,
    onBackPress: () -> Unit,
    statusBarHeight: Float,
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
                userStatus = uiState.userStatus,
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
                    modifier = Modifier,
                    contentHeight = boxWithConstraintsScope.maxHeight - headerMinHeight.dp
                )
            }
        }
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
                status = UserStatus.Online,
                lastSeen = 0,
            ),
        ),
        onBackPress = {},
        statusBarHeight = 12f,
    )
}