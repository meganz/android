package mega.privacy.android.app.main.adapters;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.FileDocument;
import mega.privacy.android.app.main.FileStorageActivity;
import mega.privacy.android.app.main.FileStorageActivity.Mode;
import mega.privacy.android.app.utils.ThumbnailUtils;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.Constants.ICON_MARGIN_DP;
import static mega.privacy.android.app.utils.Constants.ICON_SIZE_DP;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

/*
 * Adapter for FileStorageAdapter list
 */
public class FileStorageAdapter extends RecyclerView.Adapter<FileStorageAdapter.ViewHolderFileStorage> implements OnClickListener {

    private Context context;
    private MegaApiAndroid megaApi;
    private List<FileDocument> currentFiles;
    private Mode mode;

    public FileStorageAdapter(Context context, Mode mode2) {
        this.mode = mode2;
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

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_explorer, parent, false);
        ViewHolderFileStorage holder = new ViewHolderFileStorage(v);

        holder.itemLayout = v.findViewById(R.id.file_explorer_item_layout);
        holder.itemLayout.setOnClickListener(this);
        holder.imageView = v.findViewById(R.id.file_explorer_thumbnail);

        holder.textViewFileName = v.findViewById(R.id.file_explorer_filename);
        holder.textViewFileName.setOnClickListener(this);
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

        resetImageView(holder.imageView);

        switch (mode) {
            case PICK_FOLDER:
                holder.itemLayout.setEnabled(isEnabled(position));

                if (document.isFolder()) {
                    holder.imageView.setImageResource(R.drawable.ic_folder_list);
                } else {
                    holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
                }
                break;

            case BROWSE_FILES:
                if (document.isFolder()) {
                    holder.imageView.setImageResource(R.drawable.ic_folder_list);
                } else {
                    holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
                    ThumbnailUtils.createThumbnailExplorer(context, document, holder, this.megaApi, this, position);
                }
                break;
        }
    }

    /**
     * Reset the imageview's params
     * @param imageView the imageview shows the icon and the thumbnail
     */
    public void resetImageView(ImageView imageView) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
        params.height = params.width = dp2px(ICON_SIZE_DP);
        int margin = dp2px(ICON_MARGIN_DP);
        params.setMargins(margin, margin, margin, margin);
        imageView.setLayoutParams(params);
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
                ((FileStorageActivity) context).itemClick(currentPosition);
                break;
            }
        }
    }
}
