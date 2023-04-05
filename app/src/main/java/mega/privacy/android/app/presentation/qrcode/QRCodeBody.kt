package mega.privacy.android.app.presentation.qrcode

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.MyQrcodeFragmentLayoutBinding
import mega.privacy.android.app.databinding.ScancodeFragmentLayoutBinding
import mega.privacy.android.app.presentation.qrcode.model.MyQRTab
import mega.privacy.android.app.presentation.qrcode.mycode.MyCodeFragment
import mega.privacy.android.app.presentation.qrcode.mycode.model.MyQRCodeUIState
import mega.privacy.android.app.presentation.settings.exportrecoverykey.view.SNACKBAR_TEST_TAG
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.red_600_red_300
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.white
import nz.mega.documentscanner.scan.ScanFragment

/**
 * The compose UI body of QRCodeActivity
 *
 * @param qrCodeUIState UI state of QRCode page
 * @param onBackPressed handle when back is pressed
 * @param onDeleteQRCode handle when QR code deletion is performed
 * @param onResetQRCode handle when reset QR code is performed
 * @param onGotoSettings handle when settings menu is clicked
 * @param onSaveQRCode handle when save menu is clicked
 * @param onShareQRCode handle when share button is clicked
 * @param initialTab decide which [MyQRTab] to show as initial tab
 *
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalPagerApi::class)
@Composable
fun QRCodeBody(
    qrCodeUIState: MyQRCodeUIState,
    onBackPressed: () -> Unit,
    onDeleteQRCode: () -> Unit,
    onResetQRCode: () -> Unit,
    onGotoSettings: () -> Unit,
    onSaveQRCode: () -> Unit,
    onShareQRCode: () -> Unit,
    initialTab: MyQRTab,
) {
    val isLight = MaterialTheme.colors.isLight
    val tabs = MyQRTab.values().asList()
    var showMoreMenu by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = initialTab.ordinal
    )
    val snackBarHostState = remember { SnackbarHostState() }
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = {
            SnackbarHost(hostState = it) { data ->
                Snackbar(
                    modifier = Modifier.testTag(SNACKBAR_TEST_TAG),
                    snackbarData = data,
                    backgroundColor = black.takeIf { isLight } ?: white
                )
            }
        },
        topBar = {
            MyQRCodeHeader(
                qrCodeUIState = qrCodeUIState,
                showMoreMenu = showMoreMenu,
                onShowMoreClicked = { showMoreMenu = !showMoreMenu },
                onMenuDismissed = { showMoreMenu = false },
                pagerState = pagerState,
                onSave = {
                    onSaveQRCode()
                    showMoreMenu = false
                },
                onGotoSettings = {
                    onGotoSettings()
                },
                onResetQRCode = {
                    onResetQRCode()
                    showMoreMenu = false
                },
                onDeleteQRCode = {
                    onDeleteQRCode()
                    showMoreMenu = false
                },
                onBackPressed = onBackPressed,
                onShare = onShareQRCode,
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MyQRTabs(
                tabs = tabs,
                pagerState = pagerState
            )

            TabContent(tabs = tabs, pagerState = pagerState)
        }
    }

    SnackbarHost(modifier = Modifier.padding(8.dp), hostState = snackBarHostState)
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun MyQRTabs(
    modifier: Modifier = Modifier,
    tabs: List<MyQRTab>,
    pagerState: PagerState,
) {
    val scope = rememberCoroutineScope()
    TabRow(
        modifier = modifier,
        selectedTabIndex = pagerState.currentPage,
        indicator = { tabPositions: List<TabPosition> ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                color = Color.Red
            )
        },
        backgroundColor = Color.Transparent
    ) {
        tabs.forEachIndexed { index, tab ->
            Tab(
                selected = index == pagerState.currentPage,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                enabled = true,
                text = {
                    Text(
                        text = when (tab) {
                            MyQRTab.MyQRCode -> stringResource(id = R.string.section_my_code)
                            MyQRTab.ScanQRCode -> stringResource(id = R.string.section_scan_code)
                        },
                        fontWeight = FontWeight.Medium
                    )
                },
                selectedContentColor = MaterialTheme.colors.red_600_red_300,
                unselectedContentColor = MaterialTheme.colors.textColorSecondary,
            )
        }
    }
}

/**
 * Wrap [MyCodeFragment] into a composable
 */
@Composable
private fun MyQRCodeFragmentCompose() {
    AndroidViewBinding(MyQrcodeFragmentLayoutBinding::inflate)
}

/**
 * Wrap [ScanFragment] into a composable
 */
@Composable
private fun ScanCodeFragmentCompose() {
    AndroidViewBinding(ScancodeFragmentLayoutBinding::inflate)
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun TabContent(tabs: List<MyQRTab>, pagerState: PagerState) {
    HorizontalPager(state = pagerState, count = tabs.size) { page ->
        when (page) {
            MyQRTab.MyQRCode.ordinal -> {
                MyQRCodeFragmentCompose()
            }
            MyQRTab.ScanQRCode.ordinal -> {
                ScanCodeFragmentCompose()
            }
        }
    }
}

/**
 * Header of My QR code page
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun MyQRCodeHeader(
    qrCodeUIState: MyQRCodeUIState,
    showMoreMenu: Boolean,
    pagerState: PagerState,
    onShowMoreClicked: () -> Unit,
    onMenuDismissed: () -> Unit,
    onSave: () -> Unit,
    onGotoSettings: () -> Unit,
    onResetQRCode: () -> Unit,
    onDeleteQRCode: () -> Unit,
    onBackPressed: () -> Unit,
    onShare: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.section_qr_code),
                style = MaterialTheme.typography.subtitle1,
                color = black.takeIf { isLight } ?: white,
                fontWeight = FontWeight.Medium
            )
        },
        backgroundColor = white.takeIf { isLight } ?: black,
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back_white),
                    contentDescription = null,
                    tint = black.takeIf { isLight } ?: white,
                )
            }
        },
        actions = {
            if (pagerState.currentPage != MyQRTab.MyQRCode.ordinal) return@TopAppBar

            qrCodeUIState.contactLink?.let {
                IconButton(
                    modifier = Modifier.testTag("ShareButton"),
                    onClick = onShare
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_social_share_white),
                        contentDescription = null,
                        tint = black.takeIf { isLight } ?: white,
                    )
                }
            } ?: IconButton(onClick = {}) {
                Box(modifier = Modifier)
            }

            IconButton(
                modifier = Modifier.testTag("MoreButton"),
                onClick = onShowMoreClicked
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_dots_vertical_white),
                    contentDescription = null,
                    tint = black.takeIf { isLight } ?: white,
                )
            }

            DropdownMenu(
                modifier = Modifier.testTag("DropDownMenu"),
                expanded = showMoreMenu,
                onDismissRequest = onMenuDismissed,
            ) {
                qrCodeUIState.contactLink?.let {
                    DropdownMenuItem(onClick = onSave) {
                        Text(text = stringResource(id = R.string.save_action))
                    }
                }
                DropdownMenuItem(onClick = onGotoSettings) {
                    Text(text = stringResource(id = R.string.action_settings))
                }
                DropdownMenuItem(onClick = onResetQRCode) {
                    Text(text = stringResource(id = R.string.action_reset_qr))
                }
                qrCodeUIState.contactLink?.let {
                    DropdownMenuItem(onClick = onDeleteQRCode) {
                        Text(text = stringResource(id = R.string.action_delete_qr))
                    }
                }
            }

        },
        elevation = 0.dp,
    )
}

/**
 * Preview
 */
@OptIn(ExperimentalPagerApi::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "PreviewMyQRCodeHeader")
@Composable
fun PreviewMyQRCodeHeader() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        val pagerState = rememberPagerState(
            initialPage = MyQRTab.MyQRCode.ordinal
        )
        MyQRCodeHeader(
            qrCodeUIState = MyQRCodeUIState(),
            showMoreMenu = false,
            pagerState = pagerState,
            onShowMoreClicked = {},
            onMenuDismissed = {},
            onSave = {},
            onGotoSettings = {},
            onResetQRCode = {},
            onDeleteQRCode = {},
            onBackPressed = {},
            onShare = {},
        )
    }
}
