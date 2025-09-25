package mega.privacy.android.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import mega.privacy.android.data.database.MegaDatabaseConstant

/**
 * Home widget configuration entity
 *
 * @property widgetIdentifier Unique identifier for the widget
 * @property widgetOrder Order of the widget in the home screen
 * @property enabled Whether the widget is enabled
 */
@Entity(tableName = MegaDatabaseConstant.TABLE_HOME_WIDGET_CONFIGURATION)
internal data class HomeWidgetConfigurationEntity(
    @PrimaryKey
    @ColumnInfo(name = "widget_identifier")
    val widgetIdentifier: String,
    @ColumnInfo(name = "widget_order")
    val widgetOrder: Int,
    @ColumnInfo(name = "enabled")
    val enabled: Boolean,
)
