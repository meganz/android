package mega.privacy.android.app.fragments.homepage.banner

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.zhpan.bannerview.BaseViewHolder
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.main.HomePageViewModel
import nz.mega.sdk.MegaBanner

class BannerViewHolder(itemView: View) : BaseViewHolder<MegaBanner>(itemView) {
    private lateinit var viewModel: HomePageViewModel

    override fun bindData(data: MegaBanner?, position: Int, pageSize: Int) {
        val background: SimpleDraweeView = findView(R.id.draweeView_background)
        val title: TextView = findView(R.id.textView_title)
        val description: TextView = findView(R.id.textView_description)
        val image: SimpleDraweeView = findView(R.id.draweeView_image)

        background.setImageURI(data?.imageLocation.plus(data?.backgroundImage))
        image.setImageURI(data?.imageLocation.plus(data?.image))
        title.text = data?.title
        description.text = data?.description

        (findView(R.id.imageView_dismiss) as ImageView  ).setOnClickListener {
            data?.run {
                viewModel.dismissBanner(data)
            }
        }
    }

    fun setViewModel(viewModel: HomePageViewModel) {
        this.viewModel = viewModel
    }
}