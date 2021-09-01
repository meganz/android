package mega.privacy.android.app.image

import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import androidx.core.view.doOnAttach
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.GetImageUseCase
import mega.privacy.android.app.databinding.ActivityImageViewerBinding
import mega.privacy.android.app.image.adapter.ImageViewerAdapter
import mega.privacy.android.app.image.data.ImageItem
import mega.privacy.android.app.utils.Constants.*
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.documentscanner.utils.IntentUtils.extraNotNull
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject


@AndroidEntryPoint
class ImageViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    @Inject
    lateinit var getImageUseCase: GetImageUseCase

    private val nodeHandle: Long by extraNotNull(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
    private val parentNodeHandle: Long? by extra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
    private val nodePosition: Int? by extra(INTENT_EXTRA_KEY_POSITION, INVALID_POSITION)
    private val childrenHandles: LongArray? by extra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
    private val adapter by lazy { ImageViewerAdapter(::onImageClick) }

    private var positionSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = adapter

        when {
            parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE -> {
                getImageUseCase.getImages(parentNodeHandle!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = { images ->
                            adapter.submitList(images.toList())

                            if (nodePosition != null && !positionSet) {
                                binding.viewPager.currentItem = nodePosition ?: 0
                                positionSet = true
                            }
                        },
                        onError = { error ->
                            Log.e("CACATAG", error.stackTraceToString())
                        }
                    )
            }
            childrenHandles != null && childrenHandles!!.isNotEmpty() -> {
                getImageUseCase.getImages(childrenHandles!!.toList())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = { images ->
                            adapter.submitList(images.toList()) {
                                if (!positionSet && nodePosition != null) {
                                    positionSet = true
                                    binding.viewPager.postDelayed( {
                                        Log.w("CACATAG", "Scrolling to position $nodePosition out of ${binding.viewPager.adapter!!.itemCount}")
                                        binding.viewPager.setCurrentItem(nodePosition ?: 0, false)
                                    }, 250L)
                                }
                            }
                        },
                        onError = { error ->
                            Log.e("CACATAG", error.stackTraceToString())
                        }
                    )
            }
            else -> {
                getImageUseCase.getProgressiveImage(nodeHandle)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onNext = { imageUri ->
                            val images = listOf(ImageItem(nodeHandle, "single", imageUri))
                            adapter.submitList(images)
                        },
                        onError = { error ->
                            Log.e("CACATAG", error.stackTraceToString())
                        }
                    )
            }
        }
    }

//    private fun showImage() {
//        binding.image.controller = Fresco.newDraweeControllerBuilder()
//            .setDataSourceSupplier(retainingSupplier)
//            .setOldController(binding.image.controller)
//            .build()
//
//        getImageUseCase.getProgressiveFullImage(nodeHandle)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribeBy(
//                onNext = { imageUri ->
//                    Log.e("CACATAG", "Got new image!")
//                    updateImage(imageUri)
//                },
//                onError = { error ->
//                    Log.e("CACATAG", error.stackTraceToString())
//                }
//            )
//    }
//
//    private fun updateImage(imageUri: Uri) {
//        retainingSupplier.replaceSupplier(
//            Fresco.getImagePipeline().getDataSourceSupplier(
//                ImageRequest.fromUri(imageUri),
//                null,
//                ImageRequest.RequestLevel.FULL_FETCH
//            )
//        )
//    }

    private fun onImageClick(nodeHandle: Long) {
        //do something
    }
}
