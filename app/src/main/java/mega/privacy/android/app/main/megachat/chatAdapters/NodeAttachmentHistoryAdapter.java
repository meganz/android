package mega.privacy.android.app.main.megachat.chatAdapters;

import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.TimeUtils.DATE_LONG_FORMAT;
import static mega.privacy.android.app.utils.TimeUtils.formatDateAndTime;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;
import coil.request.SuccessResult;
import coil.transform.RoundedCornersTransformation;
import coil.util.CoilUtils;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity;
import mega.privacy.android.domain.entity.node.thumbnail.ChatThumbnailRequest;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class NodeAttachmentHistoryAdapter extends RecyclerView.Adapter<NodeAttachmentHistoryAdapter.ViewHolderBrowserList> implements OnClickListener, View.OnLongClickListener {
    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    ArrayList<MegaChatMessage> messages;

    DisplayMetrics outMetrics;

    private SparseBooleanArray selectedItems;

    RecyclerView listFragment;

    boolean multipleSelect;

    ChatController cC;

    public static class ViewHolderBrowser extends RecyclerView.ViewHolder {

        public ViewHolderBrowser(View v) {
            super(v);
        }

        public ImageView savedOffline;
        public ImageView publicLinkImage;
        public TextView textViewFileName;
        public EmojiTextView textViewMessageInfo;
        public long document;
        public RelativeLayout itemLayout;
        String fullNameTitle;
        boolean nameRequestedAction = false;
    }

    public static class ViewHolderBrowserList extends NodeAttachmentHistoryAdapter.ViewHolderBrowser {

        public ViewHolderBrowserList(View v) {
            super(v);
        }

        public ImageView imageView;
        public RelativeLayout threeDotsLayout;
        public ImageView versionsIcon;
        ImageView threeDotsImageView;
    }

    public void toggleAllSelection(int pos) {
        Timber.d("position: %s", pos);
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);

        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }

        Timber.d("Adapter type is LIST");
        ViewHolderBrowserList view = (ViewHolderBrowserList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %d multiselection state: %s", pos, isMultipleSelect());
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    Timber.d("onAnimationEnd: %s", selectedItems.size());
                    if (selectedItems.size() <= 0) {
                        Timber.d("toggleAllSelection: hideMultipleSelect");

                        ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
                    }
                    Timber.d("toggleAllSelection: notified item changed");
                    notifyItemChanged(positionToflip);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        } else {
            Timber.w("NULL view pos: %s", positionToflip);
            notifyItemChanged(pos);
        }
    }

    public void toggleSelection(int pos) {
        Timber.d("position: %s", pos);

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
        Timber.d("Adapter type is LIST");
        ViewHolderBrowserList view = (ViewHolderBrowserList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            view.imageView.startAnimation(flipAnimation);

        } else {
            Timber.w("View is null - not animation");
            if (selectedItems.size() <= 0) {
                ((NodeAttachmentHistoryActivity) context).hideMultipleSelect();
            }
        }
    }

    public void selectAll() {
        for (int i = 0; i < messages.size(); i++) {
            if (!isItemChecked(i)) {
                //Exclude placeholder.
                if (messages.get(i) != null) {
                    toggleAllSelection(i);
                }
            }
        }
    }

    public void clearSelections() {
        Timber.d("clearSelections");
        for (int i = 0; i < messages.size(); i++) {
            if (isItemChecked(i)) {
                //Exclude placeholder.
                if (messages.get(i) != null) {
                    toggleAllSelection(i);
                }
            }
        }
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    /*
     * Get list of all selected messages
     */
    public ArrayList<MegaChatMessage> getSelectedMessages() {
        ArrayList<MegaChatMessage> messages = new ArrayList<MegaChatMessage>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                MegaChatMessage message = getMessageAt(selectedItems.keyAt(i));
                if (message != null) {
                    messages.add(message);
                }
            }
        }
        return messages;
    }

    public NodeAttachmentHistoryAdapter(Context _context, ArrayList<MegaChatMessage> _messages, RecyclerView recyclerView) {

        this.context = _context;
        this.messages = _messages;

        this.listFragment = recyclerView;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        cC = new ChatController(context);
    }

    public void setMessages(ArrayList<MegaChatMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public NodeAttachmentHistoryAdapter.ViewHolderBrowserList onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");
        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        Timber.d("Type: ITEM_VIEW_TYPE_LIST");

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_list, parent, false);
        ViewHolderBrowserList holderList = new ViewHolderBrowserList(v);
        holderList.itemLayout = v.findViewById(R.id.file_list_item_layout);
        holderList.imageView = v.findViewById(R.id.file_list_thumbnail);
        holderList.savedOffline = v.findViewById(R.id.file_list_saved_offline);
        holderList.publicLinkImage = v.findViewById(R.id.file_list_public_link);
        holderList.textViewFileName = v.findViewById(R.id.file_list_filename);
        holderList.textViewMessageInfo = v.findViewById(R.id.file_list_filesize);
        holderList.threeDotsLayout = v.findViewById(R.id.file_list_three_dots_layout);
        holderList.threeDotsImageView = v.findViewById(R.id.file_list_three_dots);
        holderList.versionsIcon = v.findViewById(R.id.file_list_versions_icon);
        holderList.textViewMessageInfo.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams paramsThreeDotsIcon = (RelativeLayout.LayoutParams) holderList.threeDotsImageView.getLayoutParams();
        paramsThreeDotsIcon.leftMargin = scaleWidthPx(8, outMetrics);
        holderList.threeDotsImageView.setLayoutParams(paramsThreeDotsIcon);

        holderList.textViewMessageInfo.setSelected(true);
        holderList.textViewMessageInfo.setHorizontallyScrolling(true);
        holderList.textViewMessageInfo.setFocusable(true);
        holderList.textViewMessageInfo.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        holderList.textViewMessageInfo.setMarqueeRepeatLimit(-1);
        holderList.textViewMessageInfo.setSingleLine(true);
        holderList.textViewMessageInfo.setHorizontallyScrolling(true);

        holderList.savedOffline.setVisibility(View.INVISIBLE);
        holderList.versionsIcon.setVisibility(View.GONE);
        holderList.publicLinkImage.setVisibility(View.GONE);

        holderList.itemLayout.setTag(holderList);
        holderList.itemLayout.setOnClickListener(this);
        holderList.itemLayout.setOnLongClickListener(this);

        holderList.threeDotsLayout.setTag(holderList);
        holderList.threeDotsLayout.setOnClickListener(this);

        v.setTag(holderList);
        return holderList;
    }

    public void onBindViewHolder(NodeAttachmentHistoryAdapter.ViewHolderBrowserList holder, int position) {
        Timber.d("position: %s", position);

        onBindViewHolderList(holder, position);
    }

    public void onBindViewHolderList(ViewHolderBrowserList holder, int position) {
        Timber.d("position: %s", position);
        MegaChatMessage m = (MegaChatMessage) getItem(position);
        MegaNode node = m.getMegaNodeList().get(0);

        holder.document = node.getHandle();

        holder.textViewFileName.setText(node.getName());
        holder.textViewMessageInfo.setText("");

        String date = formatDateAndTime(context, m.getTimestamp(), DATE_LONG_FORMAT);

        if (m.getUserHandle() == megaChatApi.getMyUserHandle()) {
            Timber.d("MY message handle!!: %s", m.getMsgId());
            holder.fullNameTitle = megaChatApi.getMyFullname();
        } else {

            long userHandle = m.getUserHandle();
            Timber.d("Contact message!!: %s", userHandle);

            if (((NodeAttachmentHistoryActivity) context).chatRoom.isGroup()) {

                holder.fullNameTitle = cC.getParticipantFullName(userHandle);

                if (holder.fullNameTitle == null) {
                    holder.fullNameTitle = "";
                }

                if (holder.fullNameTitle.trim().length() <= 0) {

                    Timber.w("NOT found in DB - ((ViewHolderMessageChat)holder).fullNameTitle");
                    holder.fullNameTitle = context.getString(R.string.unknown_name_label);
                    if (!(holder.nameRequestedAction)) {
                        Timber.d("Call for nonContactName: %s", m.getUserHandle());
                        holder.nameRequestedAction = true;
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context, holder, this, userHandle, ((NodeAttachmentHistoryActivity) context).chatRoom.isPreview());
                        megaChatApi.getUserFirstname(userHandle, ((NodeAttachmentHistoryActivity) context).chatRoom.getAuthorizationToken(), listener);
                        megaChatApi.getUserLastname(userHandle, ((NodeAttachmentHistoryActivity) context).chatRoom.getAuthorizationToken(), listener);
                        megaChatApi.getUserEmail(userHandle, listener);
                    } else {
                        Timber.w("Name already asked and no name received: %s", m.getUserHandle());
                    }
                }

            } else {
                holder.fullNameTitle = getTitleChat(((NodeAttachmentHistoryActivity) context).chatRoom);
            }
        }

        String secondRowInfo = context.getString(R.string.second_row_info_item_shared_file_chat, holder.fullNameTitle, date);

        holder.textViewMessageInfo.setText(secondRowInfo);
        holder.textViewMessageInfo.setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
        params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
        params.setMargins(0, 0, 0, 0);
        holder.imageView.setLayoutParams(params);

        CoilUtils.dispose(holder.imageView);
        if (!multipleSelect) {
            holder.threeDotsLayout.setVisibility(View.VISIBLE);
            holder.threeDotsLayout.setOnClickListener(this);
            Timber.d("Not multiselect");
            holder.itemLayout.setBackground(null);

            Timber.d("Check the thumb");

            if (node.hasThumbnail()) {
                Timber.d("Node has thumbnail");
                loadThumbnail(m, node, holder.imageView);
            } else {
                holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            }
        } else {
            holder.threeDotsLayout.setOnClickListener(null);
            holder.threeDotsLayout.setVisibility(View.GONE);
            Timber.d("Multiselection ON");
            if (this.isItemChecked(position)) {
                holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder);
            } else {
                Timber.d("Check the thumb");
                holder.itemLayout.setBackground(null);
                holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());

                if (node.hasThumbnail()) {
                    Timber.d("Node has thumbnail");
                    loadThumbnail(m, node, holder.imageView);
                } else {
                    holder.imageView.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                }
            }
        }
    }

    private void loadThumbnail(MegaChatMessage message, MegaNode node, ImageView target) {
        Coil.imageLoader(context).enqueue(
                new ImageRequest.Builder(context)
                        .placeholder(MimeTypeList.typeForName(node.getName()).getIconResourceId())
                        .data(new ChatThumbnailRequest(((NodeAttachmentHistoryActivity) context).chatId, message.getMsgId()))
                        .target(target)
                        .crossfade(true)
                        .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                        .listener(new ImageRequest.Listener() {
                            @Override
                            public void onSuccess(@NonNull ImageRequest request, @NonNull SuccessResult result) {
                                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) target.getLayoutParams();
                                params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                                int left = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, context.getResources().getDisplayMetrics());
                                params.setMargins(left, 0, 0, 0);

                                target.setLayoutParams(params);
                            }
                        })
                        .build()
        );
    }

    @Override
    public int getItemCount() {
        if (messages != null) {
            return messages.size();
        } else {
            return 0;
        }
    }

    public Object getItem(int position) {
        if (messages != null) {
            return messages.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();

        ViewHolderBrowser holder = (ViewHolderBrowser) v.getTag();
        int currentPosition = holder.getAdapterPosition();

        Timber.d("Current position: %s", currentPosition);

        if (currentPosition < 0) {
            Timber.w("Current position error - not valid value");
            return;
        }

        final MegaChatMessage m = (MegaChatMessage) getItem(currentPosition);
        if (m == null) {
            return;
        }
        int id = v.getId();
        if (id == R.id.file_list_three_dots_layout || id == R.id.file_grid_three_dots) {
            threeDotsClicked(currentPosition, m);
        } else if (id == R.id.file_grid_three_dots_for_file) {
            threeDotsClicked(currentPosition, m);
        } else if (id == R.id.file_list_item_layout || id == R.id.file_grid_item_layout) {
            int[] screenPosition = new int[2];
            ImageView imageView;
            imageView = v.findViewById(R.id.file_list_thumbnail);
            imageView.getLocationOnScreen(screenPosition);

            int[] dimens = new int[4];
            dimens[0] = screenPosition[0];
            dimens[1] = screenPosition[1];
            dimens[2] = imageView.getWidth();
            dimens[3] = imageView.getHeight();

            ((NodeAttachmentHistoryActivity) context).itemClick(currentPosition);
        }
    }

    public void loadPreviousMessages(ArrayList<MegaChatMessage> messages, int counter) {
        Timber.d("counter: %s", counter);
        this.messages = messages;
        notifyItemRangeInserted(messages.size() - counter, counter);
    }

    public void addMessage(ArrayList<MegaChatMessage> messages, int position) {
        Timber.d("position: %s", position);
        this.messages = messages;
        notifyItemInserted(position);
        if (position == messages.size()) {
            Timber.d("No need to update more");
        } else {
            int itemCount = messages.size() - position;
            Timber.d("Update until end - itemCount: %s", itemCount);
            notifyItemRangeChanged(position, itemCount + 1);
        }
    }

    public void removeMessage(int position, ArrayList<MegaChatMessage> messages) {
        Timber.d("Size: %s", messages.size());
        this.messages = messages;
        notifyItemRemoved(position);

        if (position == messages.size() - 1) {
            Timber.d("No need to update more");
        } else {
            int itemCount = messages.size() - position;
            Timber.d("Update until end - itemCount: %s", itemCount);
            notifyItemRangeChanged(position, itemCount);
        }
    }

    private void threeDotsClicked(int currentPosition, MegaChatMessage m) {
        Timber.d("file_list_three_dots: %s", currentPosition);
        ((NodeAttachmentHistoryActivity) context).showNodeAttachmentBottomSheet(m, currentPosition);
    }

    @Override
    public boolean onLongClick(View view) {
        Timber.d("OnLongCLick");
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();

        ViewHolderBrowser holder = (ViewHolderBrowser) view.getTag();
        int currentPosition = holder.getAdapterPosition();

        ((NodeAttachmentHistoryActivity) context).activateActionMode();
        ((NodeAttachmentHistoryActivity) context).itemClick(currentPosition);

        return true;
    }

    public MegaChatMessage getMessageAt(int position) {
        try {
            if (messages != null) {
                return messages.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        Timber.d("multipleSelect: %s", multipleSelect);
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }
}