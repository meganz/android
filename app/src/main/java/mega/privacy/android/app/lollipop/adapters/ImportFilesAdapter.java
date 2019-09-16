package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.ImportFilesFragment;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class ImportFilesAdapter extends RecyclerView.Adapter<ImportFilesAdapter.ViewHolderImportFiles>  {

    Context context;
    Object fragment;
    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;
    MegaPreferences prefs;

    List<ShareInfo> files;
    HashMap<String, String> names;

    boolean itemsVisibles = false;

    OnItemClickListener mItemClickListener;

    class ThumbnailsTask extends AsyncTask<Object, Void, Void> {

        ShareInfo file;
        ViewHolderImportFiles holder;
        Uri uri;

        @Override
        protected Void doInBackground(Object... objects) {

            file = (ShareInfo) objects[0];
            holder = (ViewHolderImportFiles) objects[1];

            if (file != null) {
                File childThumbDir = new File(ThumbnailUtils.getThumbFolder(context), ImportFilesFragment.THUMB_FOLDER);
                if (childThumbDir != null) {
                    if (!childThumbDir.exists()) {
                        childThumbDir.mkdirs();
                    }
                    File thumb = new File(childThumbDir, file.getTitle() + ".jpg");
                    if (thumb != null && thumb.exists()) {
                        uri = Uri.parse(thumb.getAbsolutePath());

                    }
                    else {
                        boolean thumbnailCreated = megaApi.createThumbnail(file.getFileAbsolutePath(), thumb.getAbsolutePath());
                        if (thumbnailCreated) {
                            uri = Uri.parse(thumb.getAbsolutePath());
                        }
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

    public ImportFilesAdapter (Context context, Object fragment, List<ShareInfo> files, HashMap<String, String> names) {
        this.context = context;
        this.fragment = fragment;
        this.files = files;
        this.names = names;

        Display display = ((FileExplorerActivityLollipop) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    ViewHolderImportFiles holder;

    @Override
    public ViewHolderImportFiles onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import, parent, false);

        holder = new ViewHolderImportFiles(v);

        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_import_layout);
        holder.thumbnail = (ImageView) v.findViewById(R.id.thumbnail_file);
        holder.name = (TextView) v.findViewById(R.id.text_file);
        holder.editButton = (RelativeLayout) v.findViewById(R.id.edit_icon_layout);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderImportFiles holder, int position) {

        ShareInfo file = (ShareInfo) getItem(position);

        holder.currentPosition = position;
        holder.name.setText(names.get(file.getTitle()));
        holder.editButton.setOnClickListener(holder);

        holder.thumbnail.setImageResource(MimeTypeList.typeForName(file.getTitle()).getIconResourceId());

        if (MimeTypeList.typeForName(file.getTitle()).isImage() || MimeTypeList.typeForName(file.getTitle()).isVideo() || MimeTypeList.typeForName(file.getTitle()).isVideoReproducible()) {
            File childThumbDir = new File(ThumbnailUtils.getThumbFolder(context), ImportFilesFragment.THUMB_FOLDER);
            if (childThumbDir != null) {
                if (!childThumbDir.exists()) {
                    childThumbDir.mkdirs();
                }
                File thumb = new File(childThumbDir, file.getTitle() + ".jpg");
                if (thumb != null && thumb.exists()) {
                    Uri uri = Uri.parse(thumb.getAbsolutePath());
                    if (uri != null) {
                        holder.thumbnail.setImageURI(uri);
                    }
                } else {
                    new ThumbnailsTask().execute(new Object[]{file, holder});
                }
            }
        }

        RelativeLayout.LayoutParams params;
        if (position >= 4 && !itemsVisibles){
            holder.itemLayout.setVisibility(View.GONE);
            params = new RelativeLayout.LayoutParams(0, 0);
            holder.itemLayout.setLayoutParams(params);
        }
        else {
            holder.itemLayout.setVisibility(View.VISIBLE);
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Util.px2dp(56, outMetrics));
            holder.itemLayout.setLayoutParams(params);
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

    public void setImportNameFiles (HashMap<String, String> names) {
        this.names = names;
        notifyDataSetChanged();
    }

    public void setItemsVisibility (boolean visibles) {
        this.itemsVisibles = visibles;
        notifyDataSetChanged();
    }

    public class ViewHolderImportFiles extends RecyclerView.ViewHolder implements View.OnClickListener {

        RelativeLayout itemLayout;
        ImageView thumbnail;
        TextView name;
        RelativeLayout editButton;
        int currentPosition;

        public ViewHolderImportFiles(View itemView) {
            super(itemView);
        }

        @Override
        public void onClick(View v) {
            if(mItemClickListener != null){
                mItemClickListener.onItemClick(v, getPosition());
            }
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view , int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener){
        this.mItemClickListener = mItemClickListener;
    }
}
