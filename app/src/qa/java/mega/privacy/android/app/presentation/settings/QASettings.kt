package mega.privacy.android.app.presentation.settings

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.navigation.QASettingsGraph
import mega.privacy.android.navigation.settings.SettingClickActionType
import mega.privacy.android.navigation.settings.SettingEntryPoint
import mega.privacy.android.navigation.settings.SettingItem
import mega.privacy.android.navigation.settings.SettingSectionHeader

internal fun qaEntryPoints(context: Context) = listOf(
    SettingEntryPoint(
        section = SettingSectionHeader.About,
        items = listOf(
            SettingItem(
                key = "QA",
                name = context.getString(R.string.settings_qa),
                descriptionValue = null,
                isEnabled = null,
                clickAction = SettingClickActionType.NavigationAction(QASettingsGraph),
                isDestructive = false
            )
        )
    )
)