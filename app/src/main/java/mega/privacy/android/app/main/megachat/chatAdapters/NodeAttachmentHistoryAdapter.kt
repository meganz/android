package mega.privacy.android.app.main.megachat.chatAdapters

import android.app.Activity
import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.util.size
import androidx.recyclerview.widget.RecyclerView
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils.dispose
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.main.listeners.ChatNonContactNameListener
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.thumbnail.ChatThumbnailRequest
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatMessage
import timber.log.Timber

class NodeAttachmentHistoryAdapter(
    private val context: Context,
    private val listFragment: RecyclerView?,
) : RecyclerView.Adapter<NodeAttachmentHistoryAdapter.ViewHolderBrowserList?>(),
    View.OnClickListener, OnLongClickListener {
    private val megaChatApi: MegaChatApiAndroid =
        ((context as Activity).application as MegaApplication).getMegaChatApi()
    var messages: ArrayList<MegaChatMessage>? = null
        set(value) {
            field = value
            Timber.d("Messages set: ${value?.size}")
            notifyDataSetChanged()
        }

    private var outMetrics: DisplayMetrics = DisplayMetrics()
    private var selectedItems: SparseBooleanArray = SparseBooleanArray()
    var multipleSelect: Boolean = false
        set(value) {
            Timber.d("multipleSelect: $field")
            if (field != value) {
                field = value
            }
            if (field) {
                selectedItems = SparseBooleanArray()
            }
        }
    private val cC: ChatController = ChatController(context)
    private val display = (context as Activity).windowManager.defaultDisplay

    init {
        display.getMetrics(outMetrics)
    }

    class ViewHolderBrowserList(v: View) : RecyclerView.ViewHolder(v) {
        val imageView: ImageView = v.findViewById(R.id.file_list_thumbnail)
        val threeDotsLayout: RelativeLayout = v.findViewById(R.id.file_list_three_dots_layout)
        val versionsIcon: ImageView = v.findViewById(R.id.file_list_versions_icon)
        val threeDotsImageView: ImageView = v.findViewById(R.id.file_list_three_dots)
        val itemLayout: RelativeLayout = v.findViewById(R.id.file_list_item_layout)
        val savedOffline: ImageView = v.findViewById(R.id.file_list_saved_offline)
        val publicLinkImage: ImageView = v.findViewById(R.id.file_list_public_link)
        val textViewFileName: TextView = v.findViewById(R.id.file_list_filename)
        val textViewMessageInfo: EmojiTextView = v.findViewById(R.id.file_list_filesize)

        var document: Long = 0
        var fullNameTitle: String = ""
        var nameRequestedAction: Boolean = false

        init {
            textViewMessageInfo.apply {
                visibility = View.VISIBLE
                isSelected = true
                setHorizontallyScrolling(true)
                isFocusable = true
                ellipsize = TextUtils.TruncateAt.MARQUEE
                marqueeRepeatLimit = -1
                isSingleLine = true
            }

            imageView.apply {
                val params = layoutParams as RelativeLayout.LayoutParams
                params.height = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    36f,
                    context.resources.displayMetrics
                ).toInt()
                params.width = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    36f,
                    context.resources.displayMetrics
                ).toInt()
                val left = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    6f,
                    context.resources.displayMetrics
                ).toInt()
                params.setMargins(left, 0, 0, 0)

                layoutParams = params
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            savedOffline.visibility = View.INVISIBLE
            versionsIcon.visibility = View.GONE
            publicLinkImage.visibility = View.GONE
        }
    }

    fun toggleAllSelection(pos: Int) {
        Timber.d("position: $pos")
        val positionToflip = pos

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: $pos")
            selectedItems.delete(pos)
        } else {
            Timber.d("PUT pos: $pos")
            selectedItems.put(pos, true)
        }

        Timber.d("Adapter type is LIST")
        val view = listFragment?.findViewHolderForLayoutPosition(pos) as ViewHolderBrowserList?
        if (view != null) {
            Timber.d("Start animation: %d multiselection state: %s", pos, multipleSelect)
            val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
            flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    Timber.d("onAnimationEnd: %s", selectedItems.size)
                    if (selectedItems.size <= 0) {
                        Timber.d("toggleAllSelection: hideMultipleSelect")

                        (context as NodeAttachmentHistoryActivity).hideMultipleSelect()
                    }
                    Timber.d("toggleAllSelection: notified item changed")
                    notifyItemChanged(positionToflip)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
            view.imageView.startAnimation(flipAnimation)
        } else {
            Timber.w("NULL view pos: %s", positionToflip)
            notifyItemChanged(pos)
        }
    }

    fun toggleSelection(pos: Int) {
        Timber.d("position: $pos")

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: $pos")
            selectedItems.delete(pos)
        } else {
            Timber.d("PUT pos: $pos")
            selectedItems.put(pos, true)
        }
        notifyItemChanged(pos)
        Timber.d("Adapter type is LIST")
        val view = listFragment?.findViewHolderForLayoutPosition(pos) as ViewHolderBrowserList?
        if (view != null) {
            Timber.d("Start animation: %s", pos)
            val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
            flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    if (selectedItems.size <= 0) {
                        (context as NodeAttachmentHistoryActivity).hideMultipleSelect()
                    }
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })

            view.imageView.startAnimation(flipAnimation)
        } else {
            Timber.w("View is null - not animation")
            if (selectedItems.size <= 0) {
                (context as NodeAttachmentHistoryActivity).hideMultipleSelect()
            }
        }
    }

    fun selectAll() {
        val allMessages = messages
        if (allMessages.isNullOrEmpty()) return

        for (i in allMessages.indices) {
            if (!isItemChecked(i)) {
                // Exclude placeholder
                toggleAllSelection(i)
            }
        }
    }

    fun clearSelections() {
        Timber.d("clearSelections")
        val allMessages = messages
        if (allMessages.isNullOrEmpty()) return
        for (i in allMessages.indices) {
            if (isItemChecked(i)) {
                toggleAllSelection(i)
            }
        }
    }

    private fun isItemChecked(position: Int): Boolean {
        return selectedItems.get(position)
    }

    val selectedItemCount: Int
        get() = selectedItems.size

    fun getSelectedItems(): MutableList<Int?> {
        val items: MutableList<Int?> = java.util.ArrayList<Int?>(selectedItems.size)
        for (i in 0..<selectedItems.size) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    val selectedMessages: ArrayList<MegaChatMessage>
        get() {
            val messages = arrayListOf<MegaChatMessage>()

            for (i in 0..<selectedItems.size) {
                if (selectedItems.valueAt(i)) {
                    val message = getMessageAt(selectedItems.keyAt(i))
                    if (message != null) {
                        messages.add(message)
                    }
                }
            }
            return messages
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBrowserList {
        Timber.d("onCreateViewHolder")

        val v =
            LayoutInflater.from(parent.context).inflate(R.layout.item_file_list, parent, false)
        val holderList = ViewHolderBrowserList(v)

        val paramsThreeDotsIcon =
            holderList.threeDotsImageView.layoutParams as RelativeLayout.LayoutParams
        paramsThreeDotsIcon.leftMargin = Util.scaleWidthPx(8, outMetrics)
        holderList.threeDotsImageView.layoutParams = paramsThreeDotsIcon

        holderList.itemLayout.tag = holderList
        holderList.itemLayout.setOnClickListener(this)
        holderList.itemLayout.setOnLongClickListener(this)

        holderList.threeDotsLayout.tag = holderList
        holderList.threeDotsLayout.setOnClickListener(this)

        v.tag = holderList
        return holderList
    }

    override fun onBindViewHolder(holder: ViewHolderBrowserList, position: Int) {
        Timber.d("position: %s", position)
        val m = getItem(position) as MegaChatMessage
        val node = m.megaNodeList.get(0)

        holder.document = node.handle

        holder.textViewFileName.text = node.name
        holder.textViewMessageInfo.text = ""

        val date =
            TimeUtils.formatDateAndTime(context, m.timestamp, TimeUtils.DATE_LONG_FORMAT)

        if (m.userHandle == megaChatApi.myUserHandle) {
            Timber.d("MY message handle: %s", m.msgId)
            holder.fullNameTitle = megaChatApi.myFullname
        } else {
            val userHandle = m.userHandle
            Timber.d("Contact message: %s", userHandle)

            val chatRoom = (context as NodeAttachmentHistoryActivity).chatRoom
            if (chatRoom == null) return

            if (chatRoom.isGroup) {
                holder.fullNameTitle = cC.getParticipantFullName(userHandle)

                if (holder.fullNameTitle.trim { it <= ' ' }.isEmpty()) {
                    Timber.w("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle")
                    holder.fullNameTitle = context.getString(R.string.unknown_name_label)
                    if (!(holder.nameRequestedAction)) {
                        Timber.d("Call for nonContactName: %s", m.userHandle)
                        holder.nameRequestedAction = true
                        val listener = ChatNonContactNameListener(
                            context,
                            holder,
                            this,
                            userHandle,
                            chatRoom.isPreview
                        )
                        megaChatApi.getUserFirstname(
                            userHandle,
                            chatRoom.authorizationToken,
                            listener
                        )
                        megaChatApi.getUserLastname(
                            userHandle,
                            chatRoom.authorizationToken,
                            listener
                        )
                        megaChatApi.getUserEmail(userHandle, listener)
                    } else {
                        Timber.w("Name already asked and no name received: ${m.userHandle}")
                    }
                }
            } else {
                holder.fullNameTitle =
                    ChatUtil.getTitleChat(chatRoom)
            }
        }

        val secondRowInfo = context.getString(
            R.string.second_row_info_item_shared_file_chat,
            holder.fullNameTitle,
            date
        )

        holder.textViewMessageInfo.text = secondRowInfo
        holder.textViewMessageInfo.visibility = View.VISIBLE

        dispose(holder.imageView)

        val iconRes = typeForName(node.name).iconResourceId

        if (!multipleSelect) {
            holder.threeDotsLayout.visibility = View.VISIBLE
            holder.threeDotsLayout.setOnClickListener(this)
            Timber.d("Not multiselect")
            holder.itemLayout.background = null

            Timber.d("Check the thumb")

            if (node.hasThumbnail()) {
                Timber.d("Node has thumbnail")
                loadThumbnail(m, holder.imageView, iconRes)
            } else {
                holder.imageView.setImageResource(iconRes)
            }
        } else {
            holder.threeDotsLayout.setOnClickListener(null)
            holder.threeDotsLayout.visibility = View.GONE
            Timber.d("Multiselection ON")
            if (this.isItemChecked(position)) {
                holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder)
            } else {
                Timber.d("Check the thumb")
                holder.itemLayout.background = null
                holder.imageView.setImageResource(iconRes)

                if (node.hasThumbnail()) {
                    Timber.d("Node has thumbnail")
                    loadThumbnail(m, holder.imageView, iconRes)
                } else {
                    holder.imageView.setImageResource(iconRes)
                }
            }
        }
    }

    private fun loadThumbnail(
        message: MegaChatMessage,
        target: ImageView,
        @DrawableRes iconRes: Int,
    ) {
        val placeholder = ContextCompat.getDrawable(
            context,
            iconRes
        )?.asImage()
        val radius = context.resources
            .getDimensionPixelSize(R.dimen.thumbnail_corner_radius)
            .toFloat()

        val imageRequest = ImageRequest.Builder(context)
            .placeholder(placeholder)
            .data(
                ChatThumbnailRequest(
                    (context as NodeAttachmentHistoryActivity).chatId,
                    message.msgId
                )
            )
            .target { image ->
                target.setImageDrawable(image.asDrawable(context.resources))
            }
            .listener(
                object : ImageRequest.Listener {
                    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                        target.background = ShapeDrawable(
                            RoundRectShape(
                                floatArrayOf(
                                    radius,
                                    radius,
                                    radius,
                                    radius,
                                    radius,
                                    radius,
                                    radius,
                                    radius
                                ),
                                null,
                                null
                            )
                        )
                    }
                }
            )
            .transformations(RoundedCornersTransformation(radius))
            .build()

        SingletonImageLoader.get(context).enqueue(imageRequest)
    }

    override fun getItemCount(): Int = messages?.size ?: 0

    fun getItem(position: Int): MegaChatMessage? = messages?.getOrNull(position)

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        ((context as Activity).application as MegaApplication).sendSignalPresenceActivity()

        val holder = v.tag as ViewHolderBrowserList
        val currentPosition = holder.adapterPosition

        Timber.d("Current position: %s", currentPosition)

        if (currentPosition < 0) {
            Timber.w("Current position error - not valid value")
            return
        }

        val m = getItem(currentPosition)
        if (m == null) {
            return
        }
        val id = v.id
        if (id == R.id.file_list_three_dots_layout || id == R.id.file_grid_three_dots) {
            threeDotsClicked(currentPosition, m)
        } else if (id == R.id.file_grid_three_dots_for_file) {
            threeDotsClicked(currentPosition, m)
        } else if (id == R.id.file_list_item_layout || id == R.id.file_grid_item_layout) {
            val screenPosition = IntArray(2)
            val imageView: ImageView = v.findViewById<ImageView>(R.id.file_list_thumbnail)
            imageView.getLocationOnScreen(screenPosition)

            val dimens = IntArray(4)
            dimens[0] = screenPosition[0]
            dimens[1] = screenPosition[1]
            dimens[2] = imageView.width
            dimens[3] = imageView.height

            (context as NodeAttachmentHistoryActivity).itemClick(currentPosition)
        }
    }

    fun loadPreviousMessages(messages: ArrayList<MegaChatMessage>?, counter: Int) {
        Timber.d("counter: %s", counter)
        this.messages = messages
        val size = messages?.size ?: 0
        notifyItemRangeInserted(size - counter, counter)
    }

    fun addMessage(messages: ArrayList<MegaChatMessage>?, position: Int) {
        Timber.d("position: %s", position)
        this.messages = messages
        val size = messages?.size ?: 0
        notifyItemInserted(position)
        if (position == size) {
            Timber.d("No need to update more")
        } else {
            val itemCount = size - position
            Timber.d("Update until end - itemCount: %s", itemCount)
            notifyItemRangeChanged(position, itemCount + 1)
        }
    }

    fun removeMessage(position: Int, messages: ArrayList<MegaChatMessage>?) {
        val size = messages?.size ?: 0
        Timber.d("Size: %s", size)
        this.messages = messages
        notifyItemRemoved(position)

        if (position == size - 1) {
            Timber.d("No need to update more")
        } else {
            val itemCount = size - position
            Timber.d("Update until end - itemCount: %s", itemCount)
            notifyItemRangeChanged(position, itemCount)
        }
    }

    private fun threeDotsClicked(currentPosition: Int, m: MegaChatMessage?) {
        Timber.d("file_list_three_dots: %s", currentPosition)
        (context as NodeAttachmentHistoryActivity).showNodeAttachmentBottomSheet(m, currentPosition)
    }

    override fun onLongClick(view: View): Boolean {
        Timber.d("OnLongCLick")
        ((context as Activity).application as MegaApplication).sendSignalPresenceActivity()

        val holder = view.tag as ViewHolderBrowserList
        val currentPosition = holder.adapterPosition

        (context as NodeAttachmentHistoryActivity).activateActionMode()
        (context as NodeAttachmentHistoryActivity).itemClick(currentPosition)

        return true
    }

    fun getMessageAt(position: Int): MegaChatMessage? = messages?.getOrNull(position)
}