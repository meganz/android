package mega.privacy.android.app.presentation.featureflag

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlag
import mega.privacy.android.core.ui.controls.textfields.GenericTextField

/**
 * Calls the @FeatureFlagListContainer compose view to set the layout
 */
@Composable
fun FeatureFlagBody(
    featureFlags: List<FeatureFlag>,
    onFeatureFlagChecked: (String, Boolean) -> Unit,
    displayDescriptions: Boolean,
    filter: String?,
    onFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    useRadioButton: Boolean = false,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(10.dp),
        state = listState,
    ) {
        item {
            GenericTextField(
                placeholder = stringResource(id = R.string.settings_qa_filter),
                imeAction = ImeAction.Done,
                onTextChange = onFilterChanged,
                keyboardActions = KeyboardActions.Default,
                text = filter ?: "",
            )
        }

        items(
            items = featureFlags,
            key = { it.featureName }
        ) { (name, description, isEnabled) ->
            FeatureFlagRow(
                name = name,
                description = description.takeIf { displayDescriptions },
                isEnabled = isEnabled,
                onCheckedChange = onFeatureFlagChecked,
                useRadioButton = useRadioButton,
            )
            Divider(color = Color.Black)
        }
    }
}