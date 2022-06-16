package mega.privacy.android.app.main.megachat.chatAdapters


import android.graphics.Color
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemFileStorageCameraBinding

/**
 * RecyclerView's ViewHolder to show Camera.
 *
 * @property binding    Item's view binding
 */
class FileStorageChatCameraViewHolder(
    private val binding: ItemFileStorageCameraBinding,
) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Bind view for the Camera.
     *
     * @param hasCameraPermissions Flag to check if has camera permissions
     */
    fun bind(hasCameraPermissions: Boolean) {
        binding.apply {
            if (hasCameraPermissions) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(root.context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val cameraSelector: CameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    val preview = Preview.Builder().build()

                    cameraProvider?.unbindAll()
                    cameraProvider?.bindToLifecycle(
                        lifecycleOwner!!,
                        cameraSelector,
                        preview)

                    previewView.scaleType = PreviewView.ScaleType.FILL_START
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                }, ContextCompat.getMainExecutor(root.context))
                previewView.isVisible = true
            } else {
                previewView.isVisible = false
                cardLayout.setCardBackgroundColor(Color.BLACK)
            }
        }
    }
}
