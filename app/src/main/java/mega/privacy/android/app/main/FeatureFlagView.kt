package mega.privacy.android.app.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.main.ui.theme.AndroidTheme

@Preview
@Composable
fun previewFeatureFlagList() {
    AndroidTheme {
        Scaffold(
            topBar = {
                SearchBar()
            }, content = {
                FeatureList()
            })
    }
}

@Preview
@Composable
fun SearchBar() {
    TextField(
        value = "",
        onValueChange = {},
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        placeholder = {
            Text(text = "Search")
        },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = MaterialTheme.colors.surface
        ),
        modifier = Modifier
            .heightIn(min = 56.dp)
            .padding(8.dp)
            .fillMaxWidth())
}

@Composable
fun FeatureList() {
    val list = remember { getFeatures().toMutableList() }
    LazyColumn {
        items(items = list) { (name, isEnabled) ->
            var isChecked by rememberSaveable { mutableStateOf(isEnabled) }
            FeatureRow(name = name,
                isEnabled = isChecked,
                onCheckedChange = { newValue -> isChecked = newValue })
            Divider(color = Color.Black)
        }
    }
}

@Composable
fun FeatureRow(name: String, isEnabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier
        .padding(10.dp)
        .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = name,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterVertically))
        Switch(
            checked = isEnabled,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun getFeatures() = List(30) { i -> Feature("Feature # $i", false) }

@Composable
fun OpenPrefDialog() {
    val openDialog = rememberSaveable { mutableStateOf(true) }
    AlertDialog(onDismissRequest = { openDialog.value = false },
        title = { Text("Feature Flags") },
        confirmButton = {
            TextButton(onClick = {})
            { Text(text = "OK") }
        },
        dismissButton = {
            TextButton(onClick = {})
            { Text(text = "Cancel") }
        })
}