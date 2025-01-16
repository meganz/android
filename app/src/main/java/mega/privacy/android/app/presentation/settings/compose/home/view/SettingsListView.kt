package mega.privacy.android.app.presentation.settings.compose.home.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingHeaderItem
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingModelItem
import mega.privacy.android.app.presentation.settings.compose.home.model.SettingsUiState
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

@Composable
internal fun SettingsListView(
    data: SettingsUiState.Data,
    initialKey: String?,
    navHostController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val initialItemIndex by remember {
        derivedStateOf {
            initialKey?.let { itemKey ->
                data.settings.indexOfFirst { it.key == itemKey }
            } ?: 0
        }
    }

    val lazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialItemIndex
    )

    LazyColumn(
        modifier = modifier,
        state = lazyListState
    ) {
        items(data.settings) { item ->
            when (item) {
                is SettingHeaderItem -> {
                    SettingsSectionHeaderPlaceHolder(
                        item.headerText,
                        Modifier.testTag(settingsHeaderTag(item.key)),
                    )
                }

                is SettingModelItem -> {
                    SettingItemViewPlaceHolder(
                        settingItem = item,
                        modifier = Modifier.testTag(settingsItemTag(item.key)),
                        navHostController = navHostController,
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsSectionHeaderPlaceHolder(
    sectionHeader: @Composable () -> String,
    modifier: Modifier,
) {
    Text(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth(),
        text = sectionHeader(),
    )
}

@Composable
internal fun SettingItemViewPlaceHolder(
    settingItem: SettingModelItem,
    modifier: Modifier,
    navHostController: NavHostController,
) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clickable {
                settingItem.onClick(navHostController)
            },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            if (settingItem.isDestructive) Text(settingItem.name, color = Color.Red) else Text(
                settingItem.name
            )
            settingItem.description?.let { Text(it) }
        }
        settingItem.isEnabled?.let {
            Switch(checked = it() ?: false, onCheckedChange = null)
        }
    }
}

@Composable
@CombinedThemePreviews
private fun SettingsListViewPreview() {
    val data = SettingsUiState.Data(
        settings = persistentListOf(
            SettingHeaderItem(
                headerText = { "Example header" },
                key = "header"
            ),
            SettingModelItem(
                key = "single line",
                name = "Single line",
                description = null,
                isEnabled = null,
                isDestructive = false,
                onClick = {},
            ),
            SettingModelItem(
                key = "single line toggle",
                name = "Single line Toggle",
                description = null,
                isEnabled = { true },
                isDestructive = false,
                onClick = {},
            ),
            SettingModelItem(
                key = "two line",
                name = "Two line",
                description = "The description",
                isEnabled = null,
                isDestructive = false,
                onClick = {},
            ),
            SettingModelItem(
                key = "two line toggle",
                name = "Two line toggle",
                description = "The description",
                isEnabled = { true },
                isDestructive = false,
                onClick = {},
            ),
            SettingModelItem(
                key = "destructive",
                name = "Destructive",
                description = null,
                isEnabled = null,
                isDestructive = true,
                onClick = {},
            ),
        )
    )
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        SettingsListView(
            data = data,
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxSize(),
            initialKey = null,
            navHostController = rememberNavController(),
        )
    }
}

internal fun settingsHeaderTag(key: String) = "settings_list_view:header_item_$key"
internal fun settingsItemTag(key: String) = "settings_list_view:settingItem_item_$key"