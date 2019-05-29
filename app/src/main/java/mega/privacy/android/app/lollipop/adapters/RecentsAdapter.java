package mega.privacy.android.app.lollipop.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import mega.privacy.android.app.components.scrollBar.SectionTitleProvider;
import nz.mega.sdk.MegaApiAndroid;

public class RecentsAdapter extends RecyclerView.Adapter<RecentsAdapter.ViewHolderBucket> implements View.OnClickListener, SectionTitleProvider {

    Context context;
    MegaApiAndroid megaApi;

    DisplayMetrics outMetrics;

    public class ViewHolderBucket extends RecyclerView.ViewHolder {

        ImageView imageThumbnail;
        TextView title;
        TextView addedBy;
        TextView subtitle;
        ImageView sharedIcon;
        ImageView actionIcon;
        TextView time;
        ImageView threeDots;
        RecyclerView mediaRecycler;

        public ViewHolderBucket(View itemView) {
            super(itemView);
        }
    }

    @Override
    public RecentsAdapter.ViewHolderBucket onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecentsAdapter.ViewHolderBucket holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public String getSectionTitle(int position) {
        return null;
    }
}
