package mega.privacy.android.app.fragments.homepage.banner

import android.widget.ImageView
import android.widget.TextView
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomePageViewModel
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.debounceClick
import mega.privacy.android.domain.entity.banner.Banner
import timber.log.Timber

class BannerAdapter(private val viewModel: HomePageViewModel) : BaseBannerAdapter<Banner>() {

    private var clickBannerCallback: ClickBannerCallback? = null

    interface ClickBannerCallback {
        fun actOnActionLink(link: String)
    }

    fun setClickBannerCallback(cb: ClickBannerCallback) {
        clickBannerCallback = cb
    }


    override fun bindData(
        holder: BaseViewHolder<Banner>,
        data: Banner?,
        position: Int,
        pageSize: Int,
    ) {
        val background: ImageView = holder.findViewById(R.id.draweeView_background)
        val title: TextView = holder.findViewById(R.id.textView_title)
        val description: TextView = holder.findViewById(R.id.textView_description)
        val image: ImageView = holder.findViewById(R.id.draweeView_image)
        val dismissImage: ImageView = holder.findViewById(R.id.imageView_dismiss)

        data?.let {
            val imageUrl = it.imageLocation.plus(it.image)
            background.load(it.imageLocation.plus(it.backgroundImage)) {
                transformations(RoundedCornersTransformation(Util.dp2px(6f).toFloat()))
            }
            image.load(imageUrl) {
                Timber.d("BannerAdapter:: Loading image: $imageUrl")
                size(Util.dp2px(100f))
                listener(
                    onError = { _, error ->
                        Timber.e("BannerAdapter:: Error loading image: $error")
                    },
                    onSuccess = { _, _ ->
                        Timber.d("BannerAdapter:: Successfully loaded image: $imageUrl")
                    }
                )
            }
            title.text = it.title
            description.text = it.description

            dismissImage.debounceClick { viewModel.dismissBanner(it) }
            holder.itemView.debounceClick {
                clickBannerCallback?.actOnActionLink(it.url)
            }
        }
    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.item_banner_view
    }
}