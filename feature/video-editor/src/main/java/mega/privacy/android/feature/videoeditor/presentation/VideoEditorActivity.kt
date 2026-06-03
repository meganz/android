package mega.privacy.android.feature.videoeditor.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.core.sharedcomponents.container.AppContainerProvider
import javax.inject.Inject

/**
 * Wrapper activity that hosts the video editor screen.
 *
 * Acts as a bridge so legacy activities (e.g. the image/media viewer) can open the
 * video editor without depending on the navigation graph.
 */
@AndroidEntryPoint
class VideoEditorActivity : AppCompatActivity() {

    @Inject
    lateinit var appContainerProvider: AppContainerProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            appContainerProvider.buildSharedAppContainer(
                context = this,
                useLegacyStatusBarColor = false,
                includePsa = false,
            ) {
                VideoEditorScreen()
            }
        )
    }

    companion object {
        /**
         * Extra carrying the MEGA node handle of the video to edit.
         */
        const val EXTRA_NODE_HANDLE = "EXTRA_NODE_HANDLE"

        /**
         * Builds an [Intent] to open the video editor for the given node.
         *
         * @param context The context used to build the intent.
         * @param nodeHandle The MEGA node handle of the video to edit.
         */
        fun getIntent(context: Context, nodeHandle: Long): Intent =
            Intent(context, VideoEditorActivity::class.java).apply {
                putExtra(EXTRA_NODE_HANDLE, nodeHandle)
            }
    }
}
