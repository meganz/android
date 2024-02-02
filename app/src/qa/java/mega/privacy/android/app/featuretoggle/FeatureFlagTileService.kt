package mega.privacy.android.app.featuretoggle

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.MonitorFeatureFlagForQuickSettingsTileUseCase
import mega.privacy.android.app.domain.usecase.SetFeatureFlag
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Service to create a quick settings Tile to be able to easily change the theme from notification quick settings section.
 * @property monitorFeatureFlagForQuickSettingsTileUseCase
 * @property setFeatureFlag
 * @property ioDispatcher
 */
@AndroidEntryPoint
class FeatureFlagTileService : TileService() {

    @Inject
    lateinit var monitorFeatureFlagForQuickSettingsTileUseCase: MonitorFeatureFlagForQuickSettingsTileUseCase

    @Inject
    lateinit var setFeatureFlag: SetFeatureFlag

    @IoDispatcher
    @Inject
    lateinit var ioDispatcher: CoroutineDispatcher

    private var readJob: Job? = null
    private var writeJob: Job? = null

    private var feature: Feature? = null

    /**
     * On Start listening event.
     */
    override fun onStartListening() {
        super.onStartListening()
        readJob?.cancel()
        readJob = SupervisorJob()
        val scope = readJob?.let { CoroutineScope(ioDispatcher + it) }
        scope?.launch {
            monitorFeatureFlagForQuickSettingsTileUseCase()
                .collect {
                    updateUI(it?.first, it?.second)
                }
        }
    }

    /**
     * On stop listening event.
     */
    override fun onStopListening() {
        super.onStopListening()
        readJob?.cancel()
    }

    /**
     * On destroy event
     */
    override fun onDestroy() {
        super.onDestroy()
        writeJob?.cancel()
        readJob?.cancel()
    }

    private fun updateUI(feature: Feature?, featureFlagActive: Boolean?) {
        this.feature = feature
        qsTile.label = feature?.name ?: "Long tap to configure"
        qsTile.contentDescription = when (featureFlagActive) {
            true -> "On"
            false -> "Off"
            null -> null
        }
        qsTile.state = when (featureFlagActive) {
            true -> Tile.STATE_ACTIVE
            false -> Tile.STATE_INACTIVE
            null -> Tile.STATE_UNAVAILABLE
        }
        qsTile.updateTile()
    }

    /**
     * On click tile event
     */
    override fun onClick() {
        super.onClick()
        writeJob?.cancel()
        writeJob = SupervisorJob()
        val scope = writeJob?.let { CoroutineScope(ioDispatcher + it) }
        scope?.launch {
            feature?.name?.let {
                setFeatureFlag(it, qsTile.state == Tile.STATE_INACTIVE)
            }
        }
    }
}
