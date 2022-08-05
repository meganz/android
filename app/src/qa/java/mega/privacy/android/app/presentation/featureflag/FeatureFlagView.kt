package mega.privacy.android.app.presentation.featureflag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.featureflag.model.FeatureFlagState
import mega.privacy.android.domain.entity.FeatureFlag

/**
 * Tag to identify this class in Unit test
 */
const val FEATURE_FLAG_LIST_TAG = "FEATURE_FLAG_LIST_TAG"

/**
 * Container to hold feature flag view
 *
 * @param uiState: @FeatureFlagState
 * @param onCheckedChange: Lambda function for to handle click
 * @param modifier: Modifier
 */
@Composable
fun FeatureFlagListContainer(
    uiState: FeatureFlagState,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier
        .padding(10.dp)
        .fillMaxWidth(1f)
        .fillMaxHeight(0.9f), shape = RoundedCornerShape(8.dp),
        content = {
            FeatureFlagList(featureFlagList = uiState.featureFlagList,
                onCheckedChange = onCheckedChange,
                modifier = modifier)
        })
}

/**
 * Constructs a list which contains the feature flag custom rom
 *
 * @param featureFlagList: List of all @FeatureFlag
 * @param onCheckedChange: Lambda function for to handle click
 * @param modifier: Modifier
 */
@Composable
fun FeatureFlagList(
    featureFlagList: List<FeatureFlag>,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(modifier = modifier.testTag(FEATURE_FLAG_LIST_TAG),
        state = rememberSaveable(featureFlagList,
            saver = LazyListState.Saver) { LazyListState() }) {
        items(items = featureFlagList) { (name, isEnabled) ->
            FeatureFlagRow(name = name,
                isEnabled = isEnabled,
                onCheckedChange = onCheckedChange, modifier)
            Divider(color = Color.Black)
        }
    }
}

/**
 * Creates one row for each element in @@FeatureFlag list
 *
 * @param name: Feature flag name
 * @param isEnabled : Value of feature flag
 * @param onCheckedChange: Lambda function for to handle click
 * @param modifier: Modifier
 */
@Composable
fun FeatureFlagRow(
    name: String,
    isEnabled: Boolean,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier,
) {
    Row(modifier = modifier
        .toggleable(value = isEnabled,
            role = Role.Switch,
            onValueChange = { onCheckedChange(name, it) })
        .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = name,
            modifier = Modifier
                .padding(10.dp)
                .wrapContentWidth()
                .align(Alignment.CenterVertically))
        Switch(
            checked = isEnabled,
            onCheckedChange = null
        )
    }
}
