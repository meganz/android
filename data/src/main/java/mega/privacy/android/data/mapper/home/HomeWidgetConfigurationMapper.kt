package mega.privacy.android.data.mapper.home

import mega.privacy.android.data.database.entity.HomeWidgetConfigurationEntity
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import javax.inject.Inject

/**
 * Mapper for HomeWidgetConfiguration between data and domain layers
 */
internal class HomeWidgetConfigurationMapper @Inject constructor() {

    /**
     * Map from data entity to domain model
     */
    operator fun invoke(entity: HomeWidgetConfigurationEntity): HomeWidgetConfiguration =
        HomeWidgetConfiguration(
            widgetIdentifier = entity.widgetIdentifier,
            widgetOrder = entity.widgetOrder,
            enabled = entity.enabled,
        )

    /**
     * Map from domain model to data entity
     */
    operator fun invoke(domain: HomeWidgetConfiguration): HomeWidgetConfigurationEntity =
        HomeWidgetConfigurationEntity(
            widgetIdentifier = domain.widgetIdentifier,
            widgetOrder = domain.widgetOrder,
            enabled = domain.enabled,
        )

}
