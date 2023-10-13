package mega.privacy.android.app.presentation.imagepreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.FETCHER_PARAMS
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_NODE_FETCHER_SOURCE
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.view.ImagePreviewScreen
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class ImagePreviewActivity : BaseActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: ImagePreviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                ImagePreviewScreen(
                    onBackClicked = ::finish,
                    onSaveToDeviceClicked = {},
                )
            }
        }
    }

    companion object {
        fun createIntent(
            context: Context,
            imageSource: ImagePreviewFetcherSource,
            currentImageNodeId: Long,
            params: Map<String, Any> = mapOf(),
        ): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(IMAGE_NODE_FETCHER_SOURCE, imageSource)
                putExtra(PARAMS_CURRENT_IMAGE_NODE_ID, currentImageNodeId)
                putExtra(FETCHER_PARAMS, bundleOf(*params.toList().toTypedArray()))
            }
        }
    }
}