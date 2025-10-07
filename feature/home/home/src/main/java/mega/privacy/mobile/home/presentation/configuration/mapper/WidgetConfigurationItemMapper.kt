package mega.privacy.mobile.home.presentation.configuration.mapper

import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.mobile.home.presentation.configuration.model.WidgetConfigurationItem
import javax.inject.Inject

class WidgetConfigurationItemMapper @Inject constructor() {
    suspend operator fun invoke(
        homeWidget: HomeWidget,
        widgetConfiguration: HomeWidgetConfiguration?,
    ) = WidgetConfigurationItem(
        identifier = homeWidget.identifier,
        index = widgetConfiguration?.widgetOrder ?: homeWidget.defaultOrder,
        name = homeWidget.getWidgetName(),
        enabled = widgetConfiguration?.enabled ?: true,
        canDelete = homeWidget.canDelete,
    )
}