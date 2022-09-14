package mega.privacy.android.presentation.controls

import android.content.res.Configuration
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SearchAppBar(
    searchWidgetState: SearchWidgetState,
    typedSearch: String,
    onSearchTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    onBackPressed: () -> Unit,
    onSearchClicked: () -> Unit,
    elevation: Boolean,
    titleId: Int,
    hintId: Int,
) {
    when (searchWidgetState) {
        SearchWidgetState.COLLAPSED -> {
            CollapsedSearchAppBar(onBackPressed = onBackPressed,
                onSearchClicked = onSearchClicked,
                elevation = elevation, titleId = titleId)
        }
        SearchWidgetState.EXPANDED -> {
            ExpandedSearchAppBar(
                text = typedSearch,
                hintId = hintId,
                onSearchTextChange = onSearchTextChange,
                onCloseClicked = onCloseClicked,
                elevation = elevation
            )
        }
    }
}

@Composable
fun CollapsedSearchAppBar(
    onBackPressed: () -> Unit,
    onSearchClicked: () -> Unit,
    elevation: Boolean,
    titleId: Int,
) {
    val iconColor = if (MaterialTheme.colors.isLight) Color.Black else Color.White

    TopAppBar(
        title = {
            Text(text = stringResource(id = titleId),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Medium)
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back button",
                    tint = iconColor)
            }
        },
        actions = {
            IconButton(onClick = { onSearchClicked() }) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search Icon",
                    tint = iconColor
                )
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp
    )
}

@Composable
fun ExpandedSearchAppBar(
    text: String,
    hintId: Int,
    onSearchTextChange: (String) -> Unit,
    onCloseClicked: () -> Unit,
    elevation: Boolean,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        elevation = if (elevation) AppBarDefaults.TopAppBarElevation else 0.dp,
        color = MaterialTheme.colors.surface
    ) {
        val focusRequester = remember { FocusRequester() }
        val iconColor = if (MaterialTheme.colors.isLight) Color.Black else Color.White

        TextField(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 5.dp, end = 5.dp)
            .focusRequester(focusRequester),
            value = text,
            onValueChange = { onSearchTextChange(it) },
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
                IconButton(onClick = onCloseClicked) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Search Icon",
                        tint = iconColor
                    )
                }
            },
            trailingIcon = {
                if (text.isEmpty()) {
                    //No icon
                } else {
                    IconButton(onClick = { onSearchTextChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Icon",
                            tint = iconColor
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchTextChange(text) }),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                cursorColor = MaterialTheme.colors.secondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ))

        DisposableEffect(Unit) {
            focusRequester.requestFocus()
            onDispose { }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "DarkAppBarPreview")
@Composable
fun AppBarPreview() {
    CollapsedSearchAppBar(onBackPressed = {},
        onSearchClicked = {},
        elevation = false,
        0)
}

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