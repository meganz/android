package mega.privacy.android.app.fragments.managerFragments.cu

import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ItemCameraUploadsTitleBinding
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.TextUtil

class CuTitleViewHolder(
    private val mBinding: ItemCameraUploadsTitleBinding
) : CuGridViewHolder(mBinding.root) {

    override fun bind(node: CuNode) {
        val date = node.headerDate
        var dateText =
            if (TextUtil.isTextEmpty(date!!.second)) "[B]" + date.first + "[/B]" else StringResourcesUtils.getString(
                R.string.cu_month_year_date,
                date.first,
                date.second
            )
        try {
            dateText = dateText.replace("[B]", "<font face=\"sans-serif-medium\">")
                .replace("[/B]", "</font>")
        } catch (e: Exception) {
            LogUtil.logWarning("Exception formatting text.", e)
        }

        mBinding.headerText.text = dateText.toSpannedHtmlText()
    }

    override fun handleClick() = false
}