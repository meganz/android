package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.lollipop.ImportFileFragment;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

public class ImportFilesAdapter extends RecyclerView.Adapter<ImportFilesAdapter.ViewHolderImportFiles>  {

    Context context;
    Object fragment;

    MegaApiAndroid megaApi;
    MegaPreferences prefs;

    List<ShareInfo> files;
    HashMap<String, String> names;

    OnItemClickListener mItemClickListener;

    public ImportFilesAdapter (Context context, Object fragment, List<ShareInfo> files, HashMap<String, String> names) {
        this.context = context;
        this.fragment = fragment;
        this.files = files;
        this.names = names;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
    }

    ViewHolderImportFiles holder;

    @Override
    public ViewHolderImportFiles onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import, parent, false);

        holder = new ViewHolderImportFiles(v);

        holder.thumbnail = (ImageView) v.findViewById(R.id.thumbnail_file);
        holder.name = (TextView) v.findViewById(R.id.text_file);
        holder.editButton = (RelativeLayout) v.findViewById(R.id.edit_icon_layout);

        v.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolderImportFiles holder, int position) {

        ShareInfo file = (ShareInfo) getItem(position);

        holder.name.setText(names.get(file.getTitle()));
        holder.editButton.setOnClickListener(holder);

        holder.thumbnail.setImageResource(MimeTypeList.typeForName(file.getTitle()).getIconResourceId());
        File childThumbDir = new File(ThumbnailUtils.getThumbFolder(context), ImportFileFragment.THUMB_FOLDER);
        if(!childThumbDir.exists()){
            childThumbDir.mkdirs();
        }
        File thumb = new File(childThumbDir, file.getTitle() + ".jpg");
        boolean thumbnailCreated = false;
        thumbnailCreated = megaApi.createThumbnail(file.getFileAbsolutePath(), thumb.getAbsolutePath());
        if (thumbnailCreated) {
            Uri uri = Uri.parse(thumb.getAbsolutePath());
            if (uri != null) {
                holder.thumbnail.setImageURI(uri);
            }
        }

        holder.currentPosition = position;
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

    public class ViewHolderImportFiles extends RecyclerView.ViewHolder implements View.OnClickListener {

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

    private static void log(String log) {
        Util.log("ImportFilesAdapter", log);
    }
}
