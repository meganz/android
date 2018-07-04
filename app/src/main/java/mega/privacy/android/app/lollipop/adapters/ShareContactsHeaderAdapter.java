package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ShareContactInfo;
import nz.mega.sdk.MegaApiAndroid;

/**
 * Created by mega on 4/07/18.
 */

public class ShareContactsHeaderAdapter extends RecyclerView.Adapter<ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop> implements View.OnClickListener {

    DatabaseHandler dbH = null;
    public static int MAX_WIDTH_CONTACT_NAME_LAND=450;
    public static int MAX_WIDTH_CONTACT_NAME_PORT=200;
    private Context mContext;
    MegaApiAndroid megaApi;
    OnItemClickListener mItemClickListener;
    private List<ShareContactInfo> shareContacts;

    public ShareContactsHeaderAdapter(Context context, ArrayList<ShareContactInfo> shareContacts) {
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        mContext = context;
        this.shareContacts = shareContacts;
    }

    public void setContacts(List<ShareContactInfo> shareContacts){
        this.shareContacts = shareContacts;
        notifyDataSetChanged();

    }

    public ShareContactInfo getItem(int position)
    {
        if(position < shareContacts.size()){
            return shareContacts.get(position);
        }

        return null;
    }

    public class ViewHolderShareContactsLollipop extends RecyclerView.ViewHolder implements View.OnClickListener{

        RelativeLayout itemLayout;
        TextView contactNameTextView;
        TextView emailTextView;
        RoundedImageView imageView;
        ImageView contactStateIcon;
        int currentPosition;

        public ViewHolderShareContactsLollipop(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(mItemClickListener != null){
                mItemClickListener.onItemClick(v, getPosition());
            }
        }
    }

    @Override
    public ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ShareContactsHeaderAdapter.ViewHolderShareContactsLollipop holder, int position) {

        ShareContactInfo contact = getItem(position);

        holder.currentPosition = position;

        if (contact.getPhoneContactInfo()){

        }
        else if (contact.getMegaContactAdapter()){

        }
    }

    @Override
    public int getItemCount() {
        if (shareContacts == null) {
            return 0;
        }
        return shareContacts.size();
    }

    @Override
    public void onClick(View v) {

    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener){
        this.mItemClickListener = mItemClickListener;
    }
}
