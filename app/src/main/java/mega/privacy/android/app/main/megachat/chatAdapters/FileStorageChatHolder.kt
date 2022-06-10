package mega.privacy.android.app.main.megachat.chatAdapters


import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemFileStorageBinding
import mega.privacy.android.app.fragments.homepage.getRoundingParamsWithoutBorder
import mega.privacy.android.app.main.megachat.data.FileGalleryItem
import mega.privacy.android.app.utils.setImageRequestFromUri

/**
 * RecyclerView's ViewHolder to show FileGalleryItem.
 *
 * @property binding    Item's view binding
 */
class FileStorageChatHolder(
    private val binding: ItemFileStorageBinding,
    private val lifecycleFragment: LifecycleOwner,
) : RecyclerView.ViewHolder(binding.root) {

    /**
     * Bind view for the File storage chat toolbar.
     *
     * @param item FileGalleryItem
     * @param position Item position
     */
    fun bind(item: FileGalleryItem, position: Int) {
        binding.apply {
            takePictureLayout.isVisible = item.isTakePicture
            fileLayout.isVisible = !item.isTakePicture

            if (item.isTakePicture) {
                if (item.hasCameraPermissions == true) {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(root.context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val cameraSelector: CameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()

                        val preview = Preview.Builder()
                            .build()

                        cameraProvider?.unbindAll()
                        cameraProvider?.bindToLifecycle(
                            lifecycleFragment,
                            cameraSelector,
                            preview)

                        previewView.scaleType = PreviewView.ScaleType.FILL_START
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                    }, ContextCompat.getMainExecutor(root.context))
                }
            } else {
                icSelected.isVisible = item.isSelected
                if (item.isImage) {
                    imageThumbnail.setImageRequestFromUri(item.fileUri)
                    imageThumbnail.isVisible = true
                    imageThumbnail.hierarchy.roundingParams =
                        getRoundingParamsWithoutBorder(root.context)
                    videoDuration.isVisible = false
                    videoThumbnail.isVisible = false
                } else {
                    videoThumbnail.setImageRequestFromUri(item.fileUri)
                    videoThumbnail.isVisible = true
                    videoThumbnail.hierarchy.roundingParams =
                        getRoundingParamsWithoutBorder(root.context)
                    videoDuration.isVisible = true
                    videoDuration.text = item.duration
                    imageThumbnail.isVisible = false
                }
            }

            takePictureButton.isVisible = position == 0
        }
    }
}