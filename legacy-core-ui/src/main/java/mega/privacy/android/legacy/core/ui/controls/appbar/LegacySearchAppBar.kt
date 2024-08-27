package mega.privacy.android.legacy.core.ui.controls.appbar

import android.content.res.Configuration
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import mega.privacy.android.legacy.core.ui.R
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState
import mega.privacy.android.shared.original.core.ui.controls.appbar.ProvideDefaultMegaAppBarColors
import mega.privacy.android.shared.original.core.ui.controls.menus.MenuActions
import mega.privacy.android.shared.original.core.ui.model.MenuAction


/**
 * The compose view for search app bar
 */
@Composable
@Deprecated(
    message = "This component doesn't follow our design system correctly. " +
            "Create a new SearchAppBar following the correct approach.",
    replaceWith = ReplaceWith("SearchAppBar")
)
fun LegacySearchAppBar(
    searchWidgetState: SearchWidgetState,
    typedSearch: String,
    onSearchTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onSearchClicked: () -> Unit,
    elevation: Boolean,
    title: String,
    hintId: Int,
    modifier: Modifier = Modifier,
    isHideAfterSearch: Boolean = false,
    actions: List<MenuAction>? = null,
    leadingActions: List<MenuAction>? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
) {
    when (searchWidgetState) {
        SearchWidgetState.COLLAPSED -> {
            ProvideDefaultMegaAppBarColors {
                CollapsedSearchAppBar(
                    onBackPressed = onBackPressed,
                    onSearchClicked = onSearchClicked,
                    elevation = elevation,
                    title = title,
                    actions = actions,
                    leadingActions = leadingActions,
                    onActionPressed = onActionPressed,
                    modifier = modifier,
                )
            }
        }

        SearchWidgetState.EXPANDED -> {
            ExpandedSearchAppBar(
                text = typedSearch,
                hintId = hintId,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                elevation = elevation,
                modifier = modifier,
                isHideAfterSearch = isHideAfterSearch
            )
        }
    }
}

/**
 * The collapsed search app bar
 */
@Composable
fun CollapsedSearchAppBar(
    onBackPressed: () -> Unit,
    elevation: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    onSearchClicked: (() -> Unit)? = null,
    actions: List<MenuAction>? = null,
    leadingActions: List<MenuAction>? = null,
    onActionPressed: ((MenuAction) -> Unit)? = null,
    maxActionsToShow: Int = 3,
    enabled: Boolean = true,
    showSearchButton: Boolean = true,
) {
    val iconColor = if (MaterialTheme.colors.isLight) Color.Black else Color.White

    TopAppBar(
        title = {
            Text(
                modifier = Modifier.testTag(SEARCH_TOOLBAR_TITLE_VIEW_TEST_TAG),
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium,
            )
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(SEARCH_TOOLBAR_BACK_BUTTON_TEST_TAG),
                onClick = onBackPressed
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = iconColor
                )
            }
        },
        actions = {
            leadingActions?.let {
                MenuActions(
                    actions = it,
                    maxActionsToShow = maxActionsToShow,
                    enabled = enabled,
                    onActionClick = { action -> onActionPressed?.invoke(action) }
                )
            }
            if (showSearchButton) {
                IconButton(
                    modifier = Modifier.testTag(SEARCH_TOOLBAR_SEARCH_BUTTON_TEST_TAG),
                    onClick = { onSearchClicked?.invoke() },
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_search),
                        contentDescription = "Search Icon",
                        tint = iconColor
                    )
                }
            }
            actions?.let {
                MenuActions(
                    actions = actions,
                    maxActionsToShow = maxActionsToShow,
                    enabled = enabled,
                    onActionClick = { action -> onActionPressed?.invoke(action) }
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp,
        modifier = modifier
    )
}

/**
 * The expanded search app bar
 */
@Composable
fun ExpandedSearchAppBar(
    text: String,
    hintId: Int,
    onSearchTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    elevation: Boolean,
    modifier: Modifier = Modifier,
    isHideAfterSearch: Boolean = false,
) {
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(text, TextRange(text.length))
        )
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp,
        color = MaterialTheme.colors.surface
    ) {
        val initialLaunch = rememberSaveable { mutableStateOf(true) }
        val keyboardVisibleInPreviousConfiguration by keyboardAsState()
        val focusRequester = remember { FocusRequester() }
        val iconColor = if (MaterialTheme.colors.isLight) Color.Black else Color.White
        val keyboardController = LocalSoftwareKeyboardController.current
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 5.dp, end = 5.dp)
                .focusRequester(focusRequester)
                .testTag(SEARCH_TOOLBAR_TEXT_VIEW_TEST_TAG),
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onSearchTextChange(it.text)
            },
            placeholder = {
                Text(
                    modifier = Modifier.alpha(ContentAlpha.medium),
                    text = stringResource(id = hintId),
                    color = iconColor
                )
            },
            textStyle = TextStyle(fontSize = MaterialTheme.typography.subtitle1.fontSize),
            singleLine = true,
            leadingIcon = {
                IconButton(
                    modifier = Modifier.testTag(SEARCH_TOOLBAR_BACK_BUTTON_TEST_TAG),
                    onClick = onCloseClicked
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Search Icon",
                        tint = iconColor
                    )
                }
            },
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(
                        modifier = Modifier.testTag(SEARCH_TOOLBAR_CLOSE_BUTTON_TEST_TAG),
                        onClick = {
                            textFieldValue = TextFieldValue()
                            onSearchTextChange("")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Icon",
                            tint = iconColor
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onSearchTextChange(text)
                if (isHideAfterSearch) {
                    keyboardController?.hide()
                }
            }),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.secondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        SideEffect {
            if (initialLaunch.value || keyboardVisibleInPreviousConfiguration) {
                initialLaunch.value = false
                focusRequester.requestFocus()
            }
        }
    }
}

@Composable
private fun keyboardAsState(): State<Boolean> {
    val view = LocalView.current
    var isImeVisible by remember { mutableStateOf(false) }

    DisposableEffect(LocalWindowInfo.current) {
        val listener = ViewTreeObserver.OnPreDrawListener {
            isImeVisible = ViewCompat.getRootWindowInsets(view)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
            true
        }
        view.viewTreeObserver.addOnPreDrawListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnPreDrawListener(listener)
        }
    }
    return rememberUpdatedState(isImeVisible)
}

/**
 * App bar preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkAppBarPreview")
@Composable
fun AppBarPreview() {
    CollapsedSearchAppBar(
        onBackPressed = {},
        onSearchClicked = {},
        elevation = false,
        title = "Screen Title"
    )
}

/**
 * Search app bar preview
 */
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkSearchAppBarPreview")
@Composable
fun SearchAppBarPreview() {
    ExpandedSearchAppBar(
        text = "Some random text",
        hintId = 0,
        onSearchTextChange = {},
        onCloseClicked = {},
        elevation = false
    )
}

/**
 * Search toolbar search text view test tag
 */
const val SEARCH_TOOLBAR_TEXT_VIEW_TEST_TAG = "search_toolbar:search_text_view"

/**
 * Search toolbar close button test tag
 */
const val SEARCH_TOOLBAR_CLOSE_BUTTON_TEST_TAG = "search_toolbar:close_button"

/**
 * Search toolbar back button test tag
 */
const val SEARCH_TOOLBAR_BACK_BUTTON_TEST_TAG = "search_toolbar:back_button"

/**
 * Search toolbar search button test tag
 */
const val SEARCH_TOOLBAR_SEARCH_BUTTON_TEST_TAG = "search_toolbar:search_button"

/**
 * Search toolbar more button test tag
 */
const val SEARCH_TOOLBAR_TITLE_VIEW_TEST_TAG = "search_toolbar:title_view"