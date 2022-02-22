package mega.privacy.android.app.fragments.homepage.banner

import android.widget.ImageView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomePageViewModel
import mega.privacy.android.app.utils.ViewUtils.debounceClick
import nz.mega.sdk.MegaBanner

class BannerAdapter(private val viewModel: HomePageViewModel)
    : BaseBannerAdapter<MegaBanner>() {

    private var clickBannerCallback : ClickBannerCallback? = null

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
        pageSize: Int
    ) {
        val background: SimpleDraweeView = holder.findViewById(R.id.draweeView_background)
        val title: TextView = holder.findViewById(R.id.textView_title)
        val description: TextView = holder.findViewById(R.id.textView_description)
        val image: SimpleDraweeView = holder.findViewById(R.id.draweeView_image)
        val dismissImage: ImageView = holder.findViewById(R.id.imageView_dismiss)

        data?.let {
            background.setImageURI(it.imageLocation.plus(it.backgroundImage))
            image.setImageURI(it.imageLocation.plus(it.image))
            title.text = it.title
            description.text = it.description

            dismissImage.debounceClick { viewModel.dismissBanner(it) }
            holder.itemView.debounceClick{
                clickBannerCallback?.actOnActionLink(it.url)
            }
        }
    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.item_banner_view
    }
}