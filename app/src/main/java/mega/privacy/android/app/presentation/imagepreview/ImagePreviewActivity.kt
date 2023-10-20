package mega.privacy.android.app.presentation.imagepreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.FETCHER_PARAMS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_NODE_FETCHER_SOURCE
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID_VALUE
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.view.ImagePreviewScreen
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaNode
import javax.inject.Inject

@AndroidEntryPoint
class ImagePreviewActivity : BaseActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var imagePreviewVideoLauncher: ImagePreviewVideoLauncher

    private val viewModel: ImagePreviewViewModel by viewModels()

    private val nodeSaver: NodeSaver by lazy {
        NodeSaver(
            activityLauncher = this,
            permissionRequester = this,
            snackbarShower = this,
            confirmDialogShower = AlertsAndWarnings.showSaveToDeviceConfirmDialog(this),
        )
    }

    private val nodeAttacher: MegaAttacher by lazy { MegaAttacher(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            nodeSaver.restoreState(savedInstanceState)
            nodeAttacher.restoreState(savedInstanceState)
        }
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                ImagePreviewScreen(
                    onClickBack = ::finish,
                    onClickSaveToDevice = ::onClickSaveToDevice,
                    onClickGetLink = ::onClickGetLink,
                    onClickSendTo = ::onClickSendTo,
                    onClickVideoPlay = ::onClickVideoPlay,
                    onClickSlideshow = ::onClickSlideshow,
                )
            }
        }
    }

    private fun onClickSlideshow() {
        //TODO
        Toast.makeText(this, "Slideshow", Toast.LENGTH_SHORT).show()
    }

    private fun onClickGetLink(imageNode: ImageNode) {
        LinksUtil.showGetLinkActivity(this, imageNode.id.longValue)
    }

    private fun onClickSendTo(imageNode: ImageNode) {
        nodeAttacher.attachNode(imageNode.id.longValue)
    }

    private fun onClickVideoPlay(imageNode: ImageNode) {
        lifecycleScope.launch {
            imagePreviewVideoLauncher.launchVideoScreen(
                imageNode = imageNode,
                context = this@ImagePreviewActivity,
            )
        }
    }

    private fun onClickSaveToDevice(imageNode: ImageNode) {
        lifecycleScope.launch {
            viewModel.executeTransfer(
                transferMessage = getString(R.string.resume_paused_transfers_text)
            ) {
                saveNode(MegaNode.unserialize(imageNode.serializedData))
            }
        }
    }

    private fun saveNode(node: MegaNode) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver.saveNode(
            node,
            highPriority = false,
            isFolderLink = node.isForeign,
            fromMediaViewer = true,
            needSerialize = true,
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver.saveState(outState)
        nodeAttacher.saveState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        intent ?: return
        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return
        }
        if (nodeAttacher.handleActivityResult(requestCode, resultCode, intent, this)) {
            return
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        nodeSaver.handleRequestPermissionsResult(requestCode)
    }

    override fun onDestroy() {
        nodeSaver.destroy()
        super.onDestroy()
    }

    /**
     * End
     */

    companion object {
        fun createIntent(
            context: Context,
            imageSource: ImagePreviewFetcherSource,
            currentImageNodeId: Long,
            params: Map<String, Any> = mapOf(),
        ): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(IMAGE_NODE_FETCHER_SOURCE, imageSource)
                putExtra(PARAMS_CURRENT_IMAGE_NODE_ID_VALUE, currentImageNodeId)
                putExtra(FETCHER_PARAMS, bundleOf(*params.toList().toTypedArray()))
            }
        }
    }
}