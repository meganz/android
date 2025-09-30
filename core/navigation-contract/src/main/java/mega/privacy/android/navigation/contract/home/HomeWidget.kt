package mega.privacy.android.navigation.contract.home

import kotlinx.coroutines.flow.Flow
import mega.android.core.ui.model.LocalizedText

/**
 * Home widget
 * If the feature only provides a single widget (i.e not a file or video where you can have multiple copies of the same widget for different files),
 * then you only need to create an implementation of this interface and inject it into a set. Otherwise you need to provide an
 * implementation of [HomeWidgetProvider] (Also injected into a set)
 *
 */
interface HomeWidget {
    val identifier: String
    val defaultOrder: Int
    val canDelete: Boolean
    suspend fun getWidgetName(): LocalizedText
    fun getWidget(): Flow<HomeWidgetViewHolder>
}