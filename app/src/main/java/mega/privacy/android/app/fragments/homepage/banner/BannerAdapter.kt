package mega.privacy.android.app.fragments.homepage.banner

import android.widget.ImageView
import android.widget.TextView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomePageViewModel
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.ViewUtils.debounceClick
import nz.mega.sdk.MegaBanner

class BannerAdapter(private val viewModel: HomePageViewModel)
    : BaseBannerAdapter<MegaBanner>() {

    private var clickBannerCallback: ClickBannerCallback? = null

    interface ClickBannerCallback {
        fun actOnActionLink(link: String)
    }

    fun setClickBannerCallback(cb: ClickBannerCallback) {
        clickBannerCallback = cb
    }

    override fun bindData(
        holder: BaseViewHolder<MegaBanner>,
        data: MegaBanner?,
        position: Int,
        pageSize: Int,
    ) {
        val background: ImageView = holder.findViewById(R.id.draweeView_background)
        val title: TextView = holder.findViewById(R.id.textView_title)
        val description: TextView = holder.findViewById(R.id.textView_description)
        val image: ImageView = holder.findViewById(R.id.draweeView_image)
        val dismissImage: ImageView = holder.findViewById(R.id.imageView_dismiss)

        data?.let {
            background.load(it.imageLocation.plus(it.backgroundImage)) {
                transformations(RoundedCornersTransformation(Util.dp2px(6f).toFloat()))
            }
            image.load(it.imageLocation.plus(it.image)) {
                size(Util.dp2px(100f))
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