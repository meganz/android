package mega.privacy.android.app.main.adapters

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.target
import coil3.request.transformations
import coil3.size.Scale
import coil3.transform.RoundedCornersTransformation
import com.google.android.material.textfield.TextInputLayout
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiEditText
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.ShareTextInfo
import mega.privacy.android.domain.entity.document.DocumentEntity

internal class ImportFilesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>,
    View.OnClickListener {
    private val context: Context

    private val files: MutableList<DocumentEntity> = mutableListOf()
    private val filesAll: MutableList<DocumentEntity> = mutableListOf()
    private val filesPartial: MutableList<DocumentEntity> = mutableListOf()
    private val names = mutableMapOf<String, String>()
    private var textInfo: ShareTextInfo? = null

    private var areItemsVisible = false

    private var positionWithFocus = Constants.INVALID_POSITION

    private var onImportFilesAdapterFooterListener: OnImportFilesAdapterFooterListener? = null

    /**
     * Listener for the user action on RecycleView footer
     *
     * @param listener Instance of OnImportFilesAdapterFooterListener
     */
    fun setFooterListener(listener: OnImportFilesAdapterFooterListener?) {
        onImportFilesAdapterFooterListener = listener
    }

    /**
     * Constructor for importing files.
     *
     * @param context Context.
     * @param files   List of ShareInfo containing all the info to show the files list.
     * @param names   Map containing the original name of the files and the edited one.
     */
    constructor(context: Context, files: List<DocumentEntity>, names: Map<String, String>) {
        this.context = context
        this.filesAll.apply {
            clear()
            addAll(files)
        }
        this.names.apply {
            clear()
            putAll(names)
        }

        this.files.clear()
        if (files.size > MAX_VISIBLE_ITEMS_AT_BEGINNING) {
            filesPartial.clear()
            for (i in 0..<MAX_VISIBLE_ITEMS_AT_BEGINNING) {
                filesPartial.add(files[i])
            }
            this.files.addAll(filesPartial)
        } else {
            this.files.addAll(files)
        }
    }

    /**
     * Constructor for importing text as plain text or a link.
     *
     * @param context Context.
     * @param info    ShareTextInfo containing all the info to share the text as file or chat message.
     * @param names   Map containing the subject of the shared text as name of the file or
     * the message to share and edited value.
     */
    constructor(context: Context, info: ShareTextInfo?, names: Map<String, String>) {
        this.context = context
        this.textInfo = info
        this.names.apply {
            clear()
            putAll(names)
        }
    }

    private val contentItemCount: Int
        /**
         * Get the size of the content list
         *
         * @return the size of the list
         */
        get() {
            if (textInfo != null) {
                return 1
            }

            return files.size
        }

    override fun getItemViewType(position: Int): Int {
        val dataItemCount = contentItemCount
        return if (position >= dataItemCount) {
            // Footer view
            ITEM_TYPE_BOTTOM
        } else {
            // Content view
            ITEM_TYPE_CONTENT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == ITEM_TYPE_BOTTOM) {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_footer_import, parent, false)

            return BottomViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_import, parent, false)

            return ViewHolderImportFiles(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is BottomViewHolder) {
            val showMoreVisible =
                textInfo == null && filesAll.size > MAX_VISIBLE_ITEMS_AT_BEGINNING
            holder.showMore.visibility = if (showMoreVisible) View.VISIBLE else View.GONE
            holder.showMore.setOnClickListener { _ ->
                areItemsVisible = !areItemsVisible
                if (areItemsVisible) {
                    holder.showMoreText.text =
                        context.getString(R.string.general_show_less)
                    holder.showMoreImage.setImageResource(R.drawable.ic_expand)
                } else {
                    holder.showMoreText.text =
                        context.getString(R.string.general_show_more)
                    holder.showMoreImage.setImageResource(R.drawable.ic_collapse_acc)
                }
                setDataList(areItemsVisible)
            }
            holder.cloudDriveButton.setOnClickListener { _ -> onImportFilesAdapterFooterListener?.onClickCloudDriveButton() }
            holder.chatButton.setOnClickListener { _ -> onImportFilesAdapterFooterListener?.onClickChatButton() }
        } else {
            (holder as ViewHolderImportFiles).currentPosition = holder.getBindingAdapterPosition()
            val fileName: String

            val textInfo = this.textInfo
            if (textInfo != null) {
                fileName = textInfo.subject

                holder.separator.visibility = View.GONE
                val icon =
                    if (textInfo.isUrl) mega.privacy.android.icon.pack.R.drawable.ic_url_medium_solid else typeForName(
                        fileName
                    ).iconResourceId
                holder.thumbnail.setImageResource(icon)
            } else {
                val file = getItem(position) as DocumentEntity
                fileName = file.name

                if (typeForName(file.name).isImage
                    || typeForName(file.name).isVideo
                    || typeForName(file.name).isVideoMimeType
                ) {
                    val placeholder = ContextCompat.getDrawable(
                        context,
                        typeForName(file.name).iconResourceId
                    )?.asImage()
                    val imageRequest = ImageRequest.Builder(context)
                        .placeholder(placeholder)
                        .data(file.getUriString())
                        .size(
                            context
                                .resources
                                .getDimensionPixelSize(R.dimen.default_thumbnail_size)
                        )
                        .transformations(
                            RoundedCornersTransformation(
                                context.resources.getDimensionPixelSize(
                                    R.dimen.thumbnail_corner_radius
                                ).toFloat()
                            )
                        )
                        .scale(Scale.FILL)
                        .target(holder.thumbnail)
                        .build()
                    SingletonImageLoader.get(context).enqueue(imageRequest)
                } else {
                    holder.thumbnail.setImageResource(typeForName(file.name).iconResourceId)
                }

                if (files.size > MAX_VISIBLE_ITEMS_AT_BEGINNING) {
                    holder.separator.isGone = position == itemCount - 2
                } else {
                    holder.separator.isGone = itemCount == 2
                            || (filesAll.size > MAX_VISIBLE_ITEMS_AT_BEGINNING && position == LATEST_VISIBLE_ITEM_POSITION_AT_BEGINNING)
                }
            }

            holder.name.setText(names[fileName])
            holder.name.onFocusChangeListener =
                View.OnFocusChangeListener { v1: View?, hasFocus: Boolean ->
                    holder.editButton.visibility =
                        if (hasFocus) View.GONE else View.VISIBLE
                    if (!hasFocus) {
                        val text = holder.name.text
                        val newName = text?.toString().orEmpty()
                        names[fileName] = newName
                        (context as FileExplorerActivity).setNameFiles(names)
                        updateNameLayout(holder.nameLayout, holder.name)
                        Util.hideKeyboardView(context, v1, 0)
                    } else {
                        positionWithFocus = holder.getBindingAdapterPosition()
                    }
                }

            holder.name.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    updateNameLayout(holder.nameLayout, holder.name)
                }
            })

            holder.name.imeOptions = EditorInfo.IME_ACTION_DONE
            holder.name.setOnEditorActionListener { v: TextView, actionId: Int, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Util.hideKeyboardView(context, v, 0)
                    v.clearFocus()
                    return@setOnEditorActionListener true
                }
                false
            }

            updateNameLayout(holder.nameLayout, holder.name)
            holder.thumbnail.visibility = View.VISIBLE
        }
    }

    /**
     * Switch the data source between partial list and whole list
     *
     * @param areItemsVisible True if showing whole list, false otherwise.
     */
    private fun setDataList(areItemsVisible: Boolean) {
        files.clear()
        if (areItemsVisible) {
            files.addAll(filesAll)
            notifyItemRangeInserted(filesPartial.size, files.size - filesPartial.size)
        } else {
            files.addAll(filesPartial)
            notifyItemRangeRemoved(filesPartial.size, files.size - filesPartial.size)
        }
    }

    /**
     * Updates the view of type file name item after lost the focus by showing an error or removing it.
     *
     * @param nameLayout Input field layout.
     * @param name       Input field.
     */
    private fun updateNameLayout(nameLayout: TextInputLayout?, name: AppCompatEditText?) {
        if (nameLayout == null || name == null) {
            return
        }

        val typedName = if (name.text != null) name.text.toString() else null

        if (TextUtil.isTextEmpty(typedName)) {
            nameLayout.isErrorEnabled = true
            nameLayout.error = context.getString(R.string.empty_name)
        } else if (Constants.NODE_NAME_REGEX.matcher(typedName.orEmpty()).find()) {
            nameLayout.isErrorEnabled = true
            nameLayout.error =
                context.getString(R.string.invalid_characters)
        } else {
            nameLayout.isErrorEnabled = false
        }
    }

    override fun getItemCount(): Int {
        //The number of bottom Views
        val mBottomCount = 1

        if (files.isEmpty()) {
            return if (textInfo != null) mBottomCount + 1 else mBottomCount
        }

        return files.size + mBottomCount
    }

    fun getItem(position: Int): Any {
        return files[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    fun setImportNameFiles(names: Map<String, String>) {
        if (this.names != names) {
            this.names.apply {
                clear()
                putAll(names)
            }
            notifyDataSetChanged()
        }
    }

    /**
     * Bottom ViewHolder
     */
    inner class BottomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val showMore: RelativeLayout = itemView.findViewById(R.id.show_more_layout)
        val showMoreText: TextView = itemView.findViewById(R.id.show_more_text)
        val showMoreImage: ImageView = itemView.findViewById(R.id.show_more_image)
        val cloudDriveButton: Button = itemView.findViewById(R.id.cloud_drive_button)
        val chatButton: Button = itemView.findViewById(R.id.chat_button)
    }

    inner class ViewHolderImportFiles(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemLayout: RelativeLayout = itemView.findViewById(R.id.item_import_layout)
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail_file)
        val nameLayout: TextInputLayout = itemView.findViewById(R.id.text_file_layout)
        val name: EmojiEditText = itemView.findViewById(R.id.text_file)
        val editButton: RelativeLayout = itemView.findViewById(R.id.edit_icon_layout)
        val separator: View = itemView.findViewById(R.id.separator)
        var currentPosition: Int = 0

        init {
            // setOnClickListener to the ImportFilesAdapter
            editButton.setOnClickListener(this@ImportFilesAdapter)
            editButton.tag = this
        }
    }

    override fun onClick(v: View) {
        if (v.id != R.id.edit_icon_layout) {
            return
        }

        val holder = v.tag as? ViewHolderImportFiles
        if (holder == null || holder.name.text == null) {
            return
        }

        holder.editButton.visibility = View.GONE
        holder.name.setSelection(
            0,
            TextUtil.getCursorPositionOfName(true, holder.name.text.toString())
        )
        holder.name.requestFocus()
        Util.showKeyboardDelayed(holder.name)
    }

    /**
     * Removes the focus of the current holder selected to allow show errors if needed.
     *
     * @param list RecyclerView of the adapter.
     */
    fun updateCurrentFocusPosition(list: RecyclerView?) {
        if (positionWithFocus == Constants.INVALID_POSITION || list == null) {
            return
        }

        val holder =
            list.findViewHolderForLayoutPosition(positionWithFocus) as ViewHolderImportFiles?

        if (holder?.name == null) {
            return
        }

        holder.name.clearFocus()
        Util.hideKeyboardView(context, holder.name, 0)
        positionWithFocus = Constants.INVALID_POSITION
    }

    /**
     * This interface is to define what methods the activity
     * should implement when clicking the buttons in footer view
     */
    interface OnImportFilesAdapterFooterListener {
        /**
         * Click the cloud drive button
         */
        fun onClickCloudDriveButton()

        /**
         * Click the chat button
         */
        fun onClickChatButton()
    }

    companion object {
        const val MAX_VISIBLE_ITEMS_AT_BEGINNING: Int = 4
        private const val LATEST_VISIBLE_ITEM_POSITION_AT_BEGINNING = 3

        const val ITEM_TYPE_CONTENT: Int = 1
        const val ITEM_TYPE_BOTTOM: Int = 2
    }
}
