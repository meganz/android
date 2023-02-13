package mega.privacy.android.app.presentation.clouddrive

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.viewtype.MonitorViewType
import javax.inject.Inject

/**
 * View Model class for [mega.privacy.android.app.main.FolderLinkActivity]
 */
@HiltViewModel
class FolderLinkViewModel @Inject constructor(
    private val monitorConnectivity: MonitorConnectivity,
    private val monitorViewType: MonitorViewType,
) : ViewModel() {
    /**
     * Is connected
     */
    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Determine whether to show data in list or grid view
     */
    var isList = true

    /**
     * Flow that monitors the View Type
     */
    val onViewTypeChanged: Flow<ViewType>
        get() = monitorViewType()
}