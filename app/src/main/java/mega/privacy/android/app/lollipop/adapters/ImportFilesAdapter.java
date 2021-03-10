package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ImportFilesFragment;
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

public class ImportFilesAdapter extends RecyclerView.Adapter<ImportFilesAdapter.ViewHolderImportFiles> implements View.OnClickListener {
    public static final int MAX_VISIBLE_ITEMS_AT_BEGINNING = 4;
    private static final int LATEST_VISIBLE_ITEM_POSITION_AT_BEGINNING = 3;
    private static final int ITEM_HEIGHT = 80;

    Context context;
    Object fragment;
    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;

    List<ShareInfo> files;
    HashMap<String, String> names;

    private boolean itemsVisible = false;

    private int positionWithFocus = INVALID_POSITION;

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

    public ImportFilesAdapter(Context context, Object fragment, List<ShareInfo> files, HashMap<String, String> names) {
        this.context = context;
        this.fragment = fragment;
        this.files = files;
        this.names = names;

        Display display = ((FileExplorerActivityLollipop) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }
    }

    ViewHolderImportFiles holder;

    @NotNull
    @Override
    public ViewHolderImportFiles onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import, parent, false);

        holder = new ViewHolderImportFiles(v);

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

    @Override
    public void onBindViewHolder(ViewHolderImportFiles holder, int position) {
        ShareInfo file = (ShareInfo) getItem(position);

        holder.currentPosition = position;
        holder.name.setText(names.get(file.getTitle()));
        holder.name.setOnFocusChangeListener((v1, hasFocus) -> {
                    holder.editButton.setVisibility(hasFocus ? GONE : VISIBLE);

                    if (!hasFocus) {
                        String name = file.getTitle();
                        String newName = holder.name.getText() != null
                                ? holder.name.getText().toString() : null;

                        names.put(name, newName);
                        ((FileExplorerActivityLollipop) context).setNameFiles(names);
                        updateNameLayout(holder.nameLayout, holder.name);
                    } else {
                        positionWithFocus = position;
                    }
                }
        );

        holder.name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                holder.nameLayout.setErrorEnabled(false);
            }
        });

        holder.name.setImeOptions(EditorInfo.IME_ACTION_DONE);
        holder.name.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboardView(context, v, 0);
                v.clearFocus();
                return true;
            }

            return false;
        });

        updateNameLayout(holder.nameLayout, holder.name);

        holder.thumbnail.setVisibility(VISIBLE);

        if (typeForName(file.getTitle()).isImage()
                || typeForName(file.getTitle()).isVideo()
                || typeForName(file.getTitle()).isVideoReproducible()) {
            File thumb = getThumbnail(file);
            Uri uri = null;

            if (thumb.exists()) {
                uri = Uri.parse(thumb.getAbsolutePath());

                if (uri != null) {
                    holder.thumbnail.setImageURI(Uri.fromFile(thumb));
                }
            } else {
                new ThumbnailsTask().execute(file, holder);
            }

            if (uri == null) {
                holder.thumbnail.setImageResource(typeForName(file.getTitle()).getIconResourceId());
            }
        }

        RelativeLayout.LayoutParams params;

        if (position >= MAX_VISIBLE_ITEMS_AT_BEGINNING && !itemsVisible) {
            holder.itemLayout.setVisibility(GONE);
            params = new RelativeLayout.LayoutParams(0, 0);
        } else {
            holder.itemLayout.setVisibility(VISIBLE);
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    dp2px(ITEM_HEIGHT, outMetrics));
        }

        holder.itemLayout.setLayoutParams(params);

        if (getItemCount() > MAX_VISIBLE_ITEMS_AT_BEGINNING
                && ((itemsVisible && position == getItemCount() - 1)
                || (!itemsVisible && position == LATEST_VISIBLE_ITEM_POSITION_AT_BEGINNING))
                || getItemCount() == 1) {
            holder.separator.setVisibility(GONE);
        } else {
            holder.separator.setVisibility(VISIBLE);
        }
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
        } else if (Pattern.compile(NODE_NAME_REGEX).matcher(typedName).find()) {
            nameLayout.setErrorEnabled(true);
            nameLayout.setError(getString(R.string.invalid_characters));
        } else {
            nameLayout.setErrorEnabled(false);
        }
    }

    @Override
    public int getItemCount() {
        if (files == null) {
            return 0;
        }
        return files.size();
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

    public void setItemsVisibility(boolean visibles) {
        this.itemsVisible = visibles;
        notifyDataSetChanged();
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
}
