package mega.privacy.android.domain.entity.home

data class HomeWidgetConfiguration(
    val widgetIdentifier: String,
    val widgetOrder: Int,
    val enabled: Boolean,
)