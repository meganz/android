package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.Constants.ICON_MARGIN_DP;
import static mega.privacy.android.app.utils.Constants.ICON_SIZE_DP;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.getNumberItemChildren;
import static mega.privacy.android.app.utils.Util.getSizeString;

import android.content.Context;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import coil.Coil;
import coil.request.ImageRequest;
import coil.transform.RoundedCornersTransformation;
import coil.util.CoilUtils;
import mega.privacy.android.app.FileDocument;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity;
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity.Mode;
import mega.privacy.android.app.utils.TextUtil;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

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
            int childrenCount = document.getTotalChildren();
            String items =  context.getResources().getQuantityString(R.plurals.general_num_items, childrenCount, childrenCount);
            holder.textViewFileSize.setText(items);
        } else {
            long documentSize = document.getSize();
            holder.textViewFileSize.setText(getSizeString(documentSize, context));
        }

        holder.itemLayout.setBackgroundResource(document.isHighlighted() ? R.color.color_background_surface_2 : android.R.color.transparent);

        resetImageView(holder.imageView);

        switch (mode) {
            case PICK_FOLDER:
                holder.itemLayout.setEnabled(isEnabled(position));

                if (document.isFolder()) {
                    holder.imageView.setImageResource(mega.privacy.android.icon.pack.R.drawable.ic_folder_medium_solid);
                } else {
                    holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
                }
                break;

            case BROWSE_FILES:
                if (document.isFolder()) {
                    holder.imageView.setImageResource(mega.privacy.android.icon.pack.R.drawable.ic_folder_medium_solid);
                } else {
                    if (MimeTypeList.typeForName(document.getName()).isImage() || MimeTypeList.typeForName(document.getName()).isVideo()) {
                        Uri uri = document.getUri();

                            Coil.imageLoader(context).enqueue(
                                    new ImageRequest.Builder(context)
                                            .placeholder(MimeTypeList.typeForName(document.getName()).getIconResourceId())
                                            .data(uri)
                                            .target(holder.imageView)
                                            .transformations(new RoundedCornersTransformation(context.getResources().getDimensionPixelSize(R.dimen.thumbnail_corner_radius)))
                                            .build()
                            );
                    } else {
                        holder.imageView.setImageResource(MimeTypeList.typeForName(document.getName()).getIconResourceId());
                    }
                }
                break;
        }
    }

    /**
     * Reset the imageview's params
     *
     * @param imageView the imageview shows the icon and the thumbnail
     */
    public void resetImageView(ImageView imageView) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
        params.height = params.width = dp2px(ICON_SIZE_DP);
        int margin = dp2px(ICON_MARGIN_DP);
        params.setMargins(margin, margin, margin, margin);
        imageView.setLayoutParams(params);
        CoilUtils.dispose(imageView);
    }

    /**
     * Set new files on folder change.
     *
     * @param newFiles List of files
     */
    public void setFiles(List<FileDocument> newFiles) {
        Timber.d("setFiles");
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
     * @param position position of the item
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

        return true;
    }

    public FileDocument getItem(int position) {
        return currentFiles.get(position);
    }

    @Override
    public void onClick(View v) {
        ViewHolderFileStorage holder = (ViewHolderFileStorage) v.getTag();

        int currentPosition = holder.getAdapterPosition();

        int id = v.getId();
        if (id == R.id.file_explorer_filename || id == R.id.file_explorer_item_layout) {
            ((FileStorageActivity) context).itemClick(currentPosition);
        }
    }


    public static class CenterSmoothScroller extends LinearSmoothScroller {

        public CenterSmoothScroller(Context context) {
            super(context);
        }

        @Override
        public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
            // Calculate the distance needed to center the view
            return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
        }

        @Override
        protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
            return 50f / displayMetrics.densityDpi;
        }
    }
}

