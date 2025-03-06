package mega.privacy.android.app.presentation.settings.compose.appearance.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.settings.SettingsToggleItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.settings.compose.appearance.model.AppearanceSettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppearanceSettingsScreen(
    state: AppearanceSettingsState,
    onNavigateToMediaDiscovery: () -> Unit,
    onShowHiddenItemsToggled: (Boolean) -> Unit,
    onThemeModeSelected: (String) -> Unit,
) {
    var selectThemeMode by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    if (state is AppearanceSettingsState.Data) {
        LazyColumn(
            modifier = Modifier.padding(16.dp),
        ) {
            item {
                MegaText(
                    modifier = Modifier.padding(16.dp),
                    text = "This is a proof of concept for bottom sheet dialogs and second level navigation in the new Settings paradigm",
                    textColor = TextColor.Primary
                )
            }
            item {
                MegaText(
                    modifier = Modifier
                        .clickable {
                            selectThemeMode = true
                        }
                        .padding(16.dp),
                    text = state.themeMode, textColor = TextColor.Primary
                )
            }
            item {
                SettingsToggleItem(
                    modifier = Modifier.padding(16.dp),
                    key = "hidden",
                    title = "Show hidden items",
                    subtitle = null,
                    onSettingsChanged = { _, value -> onShowHiddenItemsToggled(value) },
                    checked = state.showHiddenItems,
                )
            }
            item {
                MegaText(
                    modifier = Modifier
                        .clickable {
                            onNavigateToMediaDiscovery()
                        }
                        .padding(16.dp),
                    text = "Media discovery view", textColor = TextColor.Primary
                )
            }
        }
    }

    if (selectThemeMode) {
        MegaModalBottomSheet(
            sheetState = sheetState,
            bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
            onDismissRequest = {
                selectThemeMode = false
            },
        ) {
            Column {
                Spacer(Modifier.height(24.dp))
                MegaText(
                    modifier = Modifier.padding(16.dp),
                    text = "This is the bottom sheet",
                    textColor = TextColor.Warning
                )
                Spacer(Modifier.height(24.dp))
                MegaText(
                    modifier = Modifier
                        .clickable {
                            onThemeModeSelected("Option 1")
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    selectThemeMode = false
                                }
                            }
                        }
                        .padding(16.dp),
                    text = "* Option 1",
                    textColor = TextColor.Primary
                )
                Spacer(Modifier.height(24.dp))
                MegaText(
                    modifier = Modifier
                        .clickable {
                            onThemeModeSelected("Option 2")
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    selectThemeMode = false
                                }
                            }
                        }
                        .padding(16.dp),
                    text = "* Option 2",
                    textColor = TextColor.Primary
                )
            }
        }
    }
}