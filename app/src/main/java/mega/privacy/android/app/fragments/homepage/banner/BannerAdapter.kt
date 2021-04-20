package mega.privacy.android.app.fragments.homepage.banner

import android.widget.ImageView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomePageViewModel
import nz.mega.sdk.MegaBanner

class BannerAdapter(private val viewModel: HomePageViewModel)
    : BaseBannerAdapter<MegaBanner>() {

    private var clickBannerCallback : ClickBannerCallback? = null

    interface ClickBannerCallback {
        fun actOnActionLink(link: String)
    }

    fun setClickBannerCallback(cb: ClickBannerCallback) {
        clickBannerCallback = cb;
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
        background.setImageURI(data?.imageLocation.plus(data?.backgroundImage))
        image.setImageURI(data?.imageLocation.plus(data?.image))
        title.text = data?.title
        description.text = data?.description

        (holder.findViewById(R.id.imageView_dismiss) as ImageView).setOnClickListener {
            data?.run {
                viewModel.dismissBanner(data)
            }
        }

        data?.url?.let { link ->
            holder.itemView.setOnClickListener {
                clickBannerCallback?.actOnActionLink(link)
            }
        }
    }

    override fun getLayoutId(viewType: Int): Int {
        return R.layout.item_banner_view
    }
}