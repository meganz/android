package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.FileDocument;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop.Mode;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

/*
 * Adapter for FilestorageActivity list
 */
public class FileStorageLollipopAdapter extends RecyclerView.Adapter<FileStorageLollipopAdapter.ViewHolderFileStorage> implements OnClickListener, View.OnLongClickListener {

    private Context context;
    private MegaApiAndroid megaApi;
    private List<FileDocument> currentFiles;
    private Mode mode;
    private RecyclerView listFragment;
    private SparseBooleanArray selectedItems;
    private boolean multipleSelect;

    public FileStorageLollipopAdapter(Context context, RecyclerView listView, Mode mode2) {
        this.mode = mode2;
        this.listFragment = listView;
        this.context = context;

        if (this.megaApi == null) {
            this.megaApi = MegaApplication.getInstance().getMegaApi();
        }
    }

    /*public view holder class*/
    public class ViewHolderFileStorage extends RecyclerView.ViewHolder {

        public ImageView imageView;
        public TextView textViewFileName;
        public TextView textViewFileSize;
        public RelativeLayout itemLayout;
        public FileDocument document;

        public ViewHolderFileStorage(View itemView) {
            super(itemView);
        }
    }

    @Override
    public ViewHolderFileStorage onCreateViewHolder(ViewGroup parent, int viewType) {

        listFragment = (RecyclerView) parent;

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_explorer, parent, false);
        ViewHolderFileStorage holder = new ViewHolderFileStorage(v);

        holder.itemLayout = v.findViewById(R.id.file_explorer_item_layout);
        holder.itemLayout.setOnClickListener(this);
        holder.itemLayout.setOnLongClickListener(this);
        holder.imageView = v.findViewById(R.id.file_explorer_thumbnail);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.imageView.getLayoutParams();
        params.setMargins(36, 0, 0, 0);
        holder.imageView.setLayoutParams(params);

        holder.textViewFileName = v.findViewById(R.id.file_explorer_filename);
        holder.textViewFileName.setOnClickListener(this);
        holder.textViewFileName.setOnLongClickListener(this);
        holder.textViewFileName.setTag(holder);
        holder.textViewFileSize = v.findViewById(R.id.file_explorer_filesize);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderFileStorage holder, int position) {

        FileDocument document = currentFiles.get(position);

        holder.textViewFileName.setText(document.getName());

        if (document.isFolder()) {
            String items = getNumberItemChildren(document.getFile());
            holder.textViewFileSize.setText(items);
        } else {
            long documentSize = document.getSize();
            holder.textViewFileSize.setText(getSizeString(documentSize));
        }

        switch (mode) {
            case PICK_FILE:
                boolean isReadable = document.getFile().canRead();
                setViewAlpha(holder.imageView, isReadable ? 1 : .4f);
                holder.textViewFileName.setTextColor(ContextCompat.getColor(context, isReadable ? android.R.color.black : R.color.text_secondary));

                boolean isReadableAndSelected = isReadable && multipleSelect && isItemChecked(position);
                holder.itemLayout.setBackgroundColor(isReadableAndSelected ? ContextCompat.getColor(context, R.color.new_multiselect_color) : Color.WHITE);


                if (document.isFolder()) {
                    holder.imageView.setImageResource(isReadableAndSelected ? R.drawable.ic_select_folder
                            : R.drawable.ic_folder_list);
                    break;
                }

                //File
                if (isReadableAndSelected) {
                    holder.imageView.setImageResource(R.drawable.ic_select_folder);
                } else {
                    holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
                    ThumbnailUtilsLollipop.createThumbnailExplorerLollipop(context, document, holder, this.megaApi, this, position);
                }

                break;

            case PICK_FOLDER:
                holder.itemLayout.setEnabled(isEnabled(position));

                if (document.isFolder()) {
                    holder.imageView.setImageResource(R.drawable.ic_folder_list);
                    setViewAlpha(holder.imageView, 1);
                    holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                } else {
                    holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
                    setViewAlpha(holder.imageView, .4f);
                    holder.textViewFileName.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                }
                break;

            case BROWSE_FILES:
                if (document.isFolder()) {
                    holder.imageView.setImageResource(R.drawable.ic_folder_list);
                } else {
                    holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
                    ThumbnailUtilsLollipop.createThumbnailExplorerLollipop(context, document, holder, this.megaApi, this, position);
                }

                holder.textViewFileName.setTextColor(ContextCompat.getColor(context, android.R.color.black));
                break;
        }
    }

    /**
     * Set new files on folder change.
     *
     * @param newFiles  List of files
     */
    public void setFiles(List<FileDocument> newFiles) {
        logDebug("setFiles");
        currentFiles = newFiles;
        notifyDataSetChanged();
    }

    public FileDocument getDocumentAt(int position) {
        if (currentFiles == null || position >= currentFiles.size()) {
            return null;
        }

        return currentFiles.get(position);
    }

    @Override
    public int getItemCount() {
        return currentFiles == null ? 0 : currentFiles.size();
    }

    /**
     * Checks if the view item has to be enabled or not.
     *
     * @param position  position of the item
     * @return True if the view item has to be enabled, false otherwise.
     */
    public boolean isEnabled(int position) {
        if (currentFiles.size() == 0) {
            return false;
        }

        FileDocument document = currentFiles.get(position);
        if (mode == Mode.PICK_FOLDER && !document.isFolder()) {
            return false;
        }

        return document.getFile().canRead();
    }

    public void toggleSelection(int pos) {
        logDebug("Position: " + pos);
        if (selectedItems.get(pos, false)) {
            logDebug("Delete pos: " + pos);
            selectedItems.delete(pos);
        } else {
            logDebug("PUT pos: " + pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);

        FileStorageLollipopAdapter.ViewHolderFileStorage view = (FileStorageLollipopAdapter.ViewHolderFileStorage) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            logDebug("Start animation: " + pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        ((FileStorageActivityLollipop) context).hideMultipleSelect();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            view.imageView.startAnimation(flipAnimation);
        }
    }

    public void toggleAllSelection(int pos) {
        logDebug("Position: " + pos);
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            logDebug("Delete pos: " + pos);
            selectedItems.delete(pos);
        } else {
            logDebug("PUT pos: " + pos);
            selectedItems.put(pos, true);
        }

        FileStorageLollipopAdapter.ViewHolderFileStorage view = (FileStorageLollipopAdapter.ViewHolderFileStorage) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            logDebug("Start animation: " + pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        ((FileStorageActivityLollipop) context).hideMultipleSelect();

                    }
                    notifyItemChanged(positionToflip);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        } else {
            logWarning("NULL view pos: " + positionToflip);
            notifyItemChanged(pos);
        }
    }

    public void selectAll() {
        for (int i = 0; i < this.getItemCount(); i++) {
            if (!isItemChecked(i)) {
                toggleSelection(i);
            }
        }
    }

    public void clearSelections() {
        logDebug("clearSelections");
        for (int i = 0; i < this.getItemCount(); i++) {
            if (isItemChecked(i)) {
                toggleAllSelection(i);
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
     * Get list of all selected nodes
     */
    public List<FileDocument> getSelectedDocuments() {
        ArrayList<FileDocument> nodes = new ArrayList<FileDocument>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i)) {
                FileDocument document = getDocumentAt(selectedItems.keyAt(i));
                if (document != null) {
                    nodes.add(document);
                }
            }
        }
        return nodes;
    }

    /*
     * Get list of all selected nodes
     */
    public int getSelectedCount() {

        if (selectedItems != null) {
            return selectedItems.size();
        }

        return -1;
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }

    public FileDocument getItem(int position) {
        return currentFiles.get(position);
    }

    @Override
    public void onClick(View v) {
        ViewHolderFileStorage holder = (ViewHolderFileStorage) v.getTag();

        int currentPosition = holder.getAdapterPosition();

        switch (v.getId()) {
            case R.id.file_explorer_filename:
            case R.id.file_explorer_item_layout: {
                ((FileStorageActivityLollipop) context).itemClick(currentPosition);
                break;
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {

        ViewHolderFileStorage holder = (ViewHolderFileStorage) view.getTag();

        switch (view.getId()) {
            case R.id.file_explorer_item_layout:
                ((FileStorageActivityLollipop) context).checkActionMode();
                ((FileStorageActivityLollipop) context).itemClick(holder.getAdapterPosition());
                break;
        }

        return true;
    }

}
