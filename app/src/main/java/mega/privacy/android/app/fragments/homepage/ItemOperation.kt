package mega.privacy.android.app.fragments.homepage

interface ItemOperation {
    fun onItemClick(item: NodeItem)
    fun showNodeItemOptions(item: NodeItem)
}