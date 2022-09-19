package mega.privacy.android.app.main.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.ImportFilesFragment;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.domain.entity.ShareTextInfo;
import nz.mega.sdk.MegaApiAndroid;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static mega.privacy.android.app.MimeTypeList.typeForName;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.NODE_NAME_REGEX;
import static mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.TextUtil.getCursorPositionOfName;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class ImportFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    public static final int MAX_VISIBLE_ITEMS_AT_BEGINNING = 4;
    private static final int LATEST_VISIBLE_ITEM_POSITION_AT_BEGINNING = 3;

    public static final int ITEM_TYPE_CONTENT = 1;
    public static final int ITEM_TYPE_BOTTOM = 2;

    Context context;

    MegaApiAndroid megaApi;

    List<ShareInfo> files;
    List<ShareInfo> filesAll;
    List<ShareInfo> filesPartial = new ArrayList<>();
    HashMap<String, String> names;
    private ShareTextInfo textInfo;

    private boolean areItemsVisible = false;

    private int positionWithFocus = INVALID_POSITION;

    private OnImportFilesAdapterFooterListener onImportFilesAdapterFooterListener;

    /**
     * Listener for the user action on RecycleView footer
     *
     * @param listener Instance of OnImportFilesAdapterFooterListener
     */
    public void setFooterListener(OnImportFilesAdapterFooterListener listener) {
        onImportFilesAdapterFooterListener = listener;
    }

    class ThumbnailsTask extends AsyncTask<Object, Void, Void> {

        ShareInfo file;
        ViewHolderImportFiles holder;
        Uri uri;

        @Override
        protected Void doInBackground(Object... objects) {
            file = (ShareInfo) objects[0];
            holder = (ViewHolderImportFiles) objects[1];

            if (file != null) {
                File thumb = getThumbnail(file);

                if (thumb.exists()) {
                    uri = Uri.parse(thumb.getAbsolutePath());
                } else {
                    boolean thumbnailCreated = megaApi.createThumbnail(file.getFileAbsolutePath(), thumb.getAbsolutePath());
                    if (thumbnailCreated) {
                        uri = Uri.parse(thumb.getAbsolutePath());
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (uri != null && holder != null) {
                holder.thumbnail.setImageURI(uri);
                if (holder.currentPosition >= 0 && holder.currentPosition < files.size()) {
                    notifyItemChanged(holder.currentPosition);
                }
            }
        }
    }

    /**
     * Gets the thumbnail if exists from the ShareInfo received.
     *
     * @param file ShareInfo to get its thumbnail.
     * @return The thumbnail if exist.
     */
    private File getThumbnail(ShareInfo file) {
        File childThumbDir = new File(getThumbFolder(context), ImportFilesFragment.THUMB_FOLDER);

        if (!childThumbDir.exists()) {
            childThumbDir.mkdirs();
        }

        return new File(childThumbDir, file.getTitle() + JPG_EXTENSION);
    }

    /**
     * Constructor for importing files.
     *
     * @param context Context.
     * @param files   List of ShareInfo containing all the info to show the files list.
     * @param names   Map containing the original name of the files and the edited one.
     */
    public ImportFilesAdapter(Context context, List<ShareInfo> files, HashMap<String, String> names) {
        this.context = context;
        this.filesAll = files;
        this.names = names;

        this.files = files;
        if (files.size() > MAX_VISIBLE_ITEMS_AT_BEGINNING) {
            filesPartial.clear();
            for (int i = 0; i < MAX_VISIBLE_ITEMS_AT_BEGINNING; i++) {
                filesPartial.add(files.get(i));
            }
            this.files = filesPartial;
        }
        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
    }

    /**
     * Constructor for importing text as plain text or a link.
     *
     * @param context Context.
     * @param info    ShareTextInfo containing all the info to share the text as file or chat message.
     * @param names   Map containing the subject of the shared text as name of the file or
     *                the message to share and edited value.
     */
    public ImportFilesAdapter(Context context, ShareTextInfo info, HashMap<String, String> names) {
        this.context = context;
        this.textInfo = info;
        this.names = names;
    }

    /**
     * Get the size of the content list
     *
     * @return the size of the list
     */
    public int getContentItemCount(){
        if (textInfo != null) {
            return 1;
        }

        if (files == null) {
            return 0;
        }

        return files.size();
    }

    /**
     * Check whether the current item is a footer view
     * @param position  The position of the item within the adapter's data set.
     * @return true if the the current item is a footer view
     */
    public boolean isBottomView(int position) {
        return position >= getContentItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        int dataItemCount = getContentItemCount();
        if (position >= dataItemCount) {
            // Footer view
            return ITEM_TYPE_BOTTOM;
        } else {
            // Content view
            return ITEM_TYPE_CONTENT;
        }
    }

    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ITEM_TYPE_BOTTOM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_footer_import, parent, false);
            BottomViewHolder bottomViewHolder = new BottomViewHolder(v);

            bottomViewHolder.showMore = v.findViewById(R.id.show_more_layout);
            bottomViewHolder.showMoreText = v.findViewById(R.id.show_more_text);
            bottomViewHolder.showMoreImage = v.findViewById(R.id.show_more_image);
            bottomViewHolder.cloudDriveButton = v.findViewById(R.id.cloud_drive_button);
            bottomViewHolder.chatButton = v.findViewById(R.id.chat_button);

            return bottomViewHolder;
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import, parent, false);
            ViewHolderImportFiles holder = new ViewHolderImportFiles(v);

            holder.itemLayout = v.findViewById(R.id.item_import_layout);
            holder.thumbnail = v.findViewById(R.id.thumbnail_file);
            holder.nameLayout = v.findViewById(R.id.text_file_layout);
            holder.name = v.findViewById(R.id.text_file);
            holder.editButton = v.findViewById(R.id.edit_icon_layout);
            holder.editButton.setOnClickListener(this);
            holder.editButton.setTag(holder);
            holder.separator = v.findViewById(R.id.separator);

            return holder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof BottomViewHolder) {
            boolean showMoreVisible = textInfo == null && filesAll.size() > MAX_VISIBLE_ITEMS_AT_BEGINNING;
            ((BottomViewHolder)holder).showMore.setVisibility(showMoreVisible ? VISIBLE : GONE);
            ((BottomViewHolder)holder).showMore.setOnClickListener(show->{
                areItemsVisible = !areItemsVisible;

                if (areItemsVisible) {
                    ((BottomViewHolder)holder).showMoreText.setText(StringResourcesUtils.getString(R.string.general_show_less));
                    ((BottomViewHolder)holder).showMoreImage.setImageResource(R.drawable.ic_expand);
                } else {
                    ((BottomViewHolder)holder).showMoreText.setText(StringResourcesUtils.getString(R.string.general_show_more));
                    ((BottomViewHolder)holder).showMoreImage.setImageResource(R.drawable.ic_collapse_acc);
                }
                setDataList(areItemsVisible);
            });
            ((BottomViewHolder) holder).cloudDriveButton.setOnClickListener(l ->
                    onImportFilesAdapterFooterListener.onClickCloudDriveButton());
            ((BottomViewHolder) holder).chatButton.setOnClickListener(l ->
                    onImportFilesAdapterFooterListener.onClickChatButton());
        } else {
            ((ViewHolderImportFiles) holder).currentPosition = holder.getBindingAdapterPosition();
            String fileName;

            if (textInfo != null) {
                fileName = textInfo.getSubject();

                ((ViewHolderImportFiles) holder).separator.setVisibility(GONE);
                int icon = textInfo.isUrl() ? R.drawable.ic_url_list : typeForName(fileName).getIconResourceId();
                ((ViewHolderImportFiles) holder).thumbnail.setImageResource(icon);
            } else {
                ShareInfo file = (ShareInfo) getItem(position);
                fileName = file.getTitle();

                Uri uri = null;

                if (typeForName(file.getTitle()).isImage()
                        || typeForName(file.getTitle()).isVideo()
                        || typeForName(file.getTitle()).isVideoReproducible()) {
                    File thumb = getThumbnail(file);

                    if (thumb.exists()) {
                        uri = Uri.parse(thumb.getAbsolutePath());

                        if (uri != null) {
                            ((ViewHolderImportFiles) holder).thumbnail.setImageURI(Uri.fromFile(thumb));
                        }
                    } else {
                        new ThumbnailsTask().execute(file, holder);
                    }
                }

                if (uri == null) {
                    ((ViewHolderImportFiles) holder).thumbnail.setImageResource(typeForName(file.getTitle()).getIconResourceId());
                }

                if (files.size() > MAX_VISIBLE_ITEMS_AT_BEGINNING) {
                    if (position == getItemCount() - 2 ) {
                        ((ViewHolderImportFiles) holder).separator.setVisibility(GONE);
                    } else {
                        ((ViewHolderImportFiles) holder).separator.setVisibility(VISIBLE);
                    }
                } else {
                    if (getItemCount() == 2){
                        ((ViewHolderImportFiles) holder).separator.setVisibility(GONE);
                    } else if (filesAll.size() > MAX_VISIBLE_ITEMS_AT_BEGINNING && position == LATEST_VISIBLE_ITEM_POSITION_AT_BEGINNING){
                        ((ViewHolderImportFiles) holder).separator.setVisibility(GONE);
                    } else {
                        ((ViewHolderImportFiles) holder).separator.setVisibility(VISIBLE);
                    }
                }
            }

            ((ViewHolderImportFiles) holder).name.setText(names.get(fileName));
            ((ViewHolderImportFiles) holder).name.setOnFocusChangeListener((v1, hasFocus) -> {
                        ((ViewHolderImportFiles) holder).editButton.setVisibility(hasFocus ? GONE : VISIBLE);

                        if (!hasFocus) {
                            Editable text = ((ViewHolderImportFiles) holder).name.getText();
                            String newName = text != null ? text.toString() : null;
                            names.put(fileName, newName);
                            ((FileExplorerActivity) context).setNameFiles(names);
                            updateNameLayout(((ViewHolderImportFiles) holder).nameLayout, ((ViewHolderImportFiles) holder).name);
                        } else {
                            positionWithFocus = holder.getBindingAdapterPosition();
                        }
                    }
            );

            ((ViewHolderImportFiles) holder).name.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    updateNameLayout(((ViewHolderImportFiles) holder).nameLayout, ((ViewHolderImportFiles) holder).name);
                }
            });

            ((ViewHolderImportFiles) holder).name.setImeOptions(EditorInfo.IME_ACTION_DONE);
            ((ViewHolderImportFiles) holder).name.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboardView(context, v, 0);
                    v.clearFocus();
                    return true;
                }

                return false;
            });

            updateNameLayout(((ViewHolderImportFiles) holder).nameLayout, ((ViewHolderImportFiles) holder).name);
            ((ViewHolderImportFiles) holder).thumbnail.setVisibility(VISIBLE);
        }
    }

    /**
     * Switch the data source between partial list and whole list
     *
     * @param areItemsVisible True if showing whole list, false otherwise.
     */
    private void setDataList(boolean areItemsVisible) {
        if(areItemsVisible) {
            files = filesAll;
        } else {
            files = filesPartial;
        }
        notifyDataSetChanged();
    }

    /**
     * Updates the view of type file name item after lost the focus by showing an error or removing it.
     *
     * @param nameLayout Input field layout.
     * @param name       Input field.
     */
    private void updateNameLayout(TextInputLayout nameLayout, AppCompatEditText name) {
        if (nameLayout == null || name == null) {
            return;
        }

        String typedName = name.getText() != null ? name.getText().toString() : null;

        if (isTextEmpty(typedName)) {
            nameLayout.setErrorEnabled(true);
            nameLayout.setError(getString(R.string.empty_name));
        } else if (NODE_NAME_REGEX.matcher(typedName).find()) {
            nameLayout.setErrorEnabled(true);
            nameLayout.setError(getString(R.string.invalid_characters));
        } else {
            nameLayout.setErrorEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        //The number of bottom Views
        int mBottomCount = 1;

        if (files == null) {
            return textInfo!= null ? mBottomCount + 1 : mBottomCount;
        }

        return files.size() + mBottomCount;
    }

    public Object getItem(int position) {
        return files.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setImportNameFiles(HashMap<String, String> names) {
        this.names = names;
        notifyDataSetChanged();
    }

    /**
     * Bottom ViewHolder
     */
    public static class BottomViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout showMore;
        TextView showMoreText;
        ImageView showMoreImage;
        Button cloudDriveButton;
        Button chatButton;
        public BottomViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class ViewHolderImportFiles extends RecyclerView.ViewHolder {

        RelativeLayout itemLayout;
        SimpleDraweeView thumbnail;
        TextInputLayout nameLayout;
        EmojiEditText name;
        RelativeLayout editButton;
        View separator;
        int currentPosition;

        public ViewHolderImportFiles(View itemView) {
            super(itemView);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.edit_icon_layout) {
            return;
        }

        ViewHolderImportFiles holder = (ViewHolderImportFiles) v.getTag();
        if (holder == null || holder.name.getText() == null) {
            return;
        }

        holder.editButton.setVisibility(GONE);
        holder.name.setSelection(0, getCursorPositionOfName(true, holder.name.getText().toString()));
        holder.name.requestFocus();
        showKeyboardDelayed(holder.name);
    }

    /**
     * Removes the focus of the current holder selected to allow show errors if needed.
     *
     * @param list RecyclerView of the adapter.
     */
    public void updateCurrentFocusPosition(RecyclerView list) {
        if (positionWithFocus == INVALID_POSITION || list == null) {
            return;
        }

        ViewHolderImportFiles holder = (ViewHolderImportFiles) list.findViewHolderForLayoutPosition(positionWithFocus);

        if (holder == null || holder.name == null) {
            return;
        }

        holder.name.clearFocus();
        hideKeyboardView(context, holder.name, 0);
        positionWithFocus = INVALID_POSITION;
    }

    /**
     * This interface is to define what methods the activity
     * should implement when clicking the buttons in footer view
     */
    public interface OnImportFilesAdapterFooterListener {
        /**
         * Click the cloud drive button
         */
        void onClickCloudDriveButton();

        /**
         * Click the chat button
         */
        void onClickChatButton();
    }
}
