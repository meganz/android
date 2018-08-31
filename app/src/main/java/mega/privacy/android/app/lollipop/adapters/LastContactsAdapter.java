package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.listeners.LastContactAvatarListener;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaUser;

public class LastContactsAdapter extends RecyclerView.Adapter<LastContactsAdapter.ViewHolder> {
    
    public static final int MAX_COLUMN = 3;
    private static final int MAX_CONTACTS = 6;
    private static final int RX = 10;
    private static final int RY = 10;
    
    private Activity context;
    
    private MegaApiAndroid megaApi;
    
    private List<MegaUser> lastContacts;
    
    private int contactsCount;
    
    private DisplayMetrics outMetrics;
    

    
    public LastContactsAdapter(final Activity context,List<MegaUser> data) {
        this.context = context;
        this.lastContacts = subLastContacts(data);
        this.contactsCount = this.lastContacts.size();
        this.lastContacts = reOrder(this.lastContacts);
        
        if (megaApi == null) {
            megaApi = ((MegaApplication)context.getApplication()).getMegaApi();
        }
    }
    
    private List<MegaUser> reOrder(List<MegaUser> lastContacts) {
        List<MegaUser> list = new ArrayList<>(MAX_CONTACTS);
        for (int i = 0;i < MAX_CONTACTS;i++) {
            list.add(i,null);
        }
        switch (contactsCount) {
            case MAX_CONTACTS:
                list.remove(3);
                list.add(3,lastContacts.get(5));
            case MAX_CONTACTS - 1:
                list.remove(0);
                list.add(0,lastContacts.get(4));
            case MAX_CONTACTS - 2:
                list.remove(4);
                list.add(4,lastContacts.get(3));
            case MAX_CONTACTS - 3:
                list.remove(1);
                list.add(1,lastContacts.get(2));
            case MAX_CONTACTS - 4:
                list.remove(5);
                list.add(5,lastContacts.get(1));
            case MAX_CONTACTS - 5:
                list.remove(2);
                list.add(2,lastContacts.get(0));
        }
        return list;
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType) {
        Display display = context.getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        
        View main = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_last_contacts,parent,false);
        ViewHolder holder = new ViewHolder(main);
        holder.avatarImage = (ImageView)main.findViewById(R.id.item_last_contacts_avatar);
        holder.avatarImage.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                ViewHolder holder = (ViewHolder)v.getTag();
                int currentPosition = holder.getAdapterPosition();
                MegaUser contact = lastContacts.get(currentPosition);
                if (contact != null) {
                    Log.e("@#@",currentPosition + "--->" + lastContacts.get(currentPosition).getEmail());
                }
            }
        });
        holder.avatarImage.setTag(holder);
        return holder;
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder,int position) {
        MegaUser contact = lastContacts.get(position);
        if (contact == null) {
            return;
        }
        Bitmap avatar = getAvatar(contact,holder.avatarImage);
        holder.avatarImage.setImageBitmap(avatar);
    }
    
    @Override
    public int getItemCount() {
        return lastContacts.size();
    }
    
    private Bitmap getAvatar(MegaUser contact,ImageView avatarImage) {
        String email = contact.getEmail();
        LastContactAvatarListener listener = new LastContactAvatarListener(context,email,avatarImage);
        return createDefaultAvatar(contact);
    }
    
    private Bitmap createDefaultAvatar(MegaUser contact) {
        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        String color = megaApi.getUserAvatarColor(contact);
        if (color != null) {
            p.setColor(Color.parseColor(color));
        } else {
            p.setColor(ContextCompat.getColor(context,R.color.lollipop_primary_color));
        }
        p.setStyle(Paint.Style.FILL);
        Path path = ThumbnailUtilsLollipop.getRoundedRect(0,0,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,10,10,true,true,false,false);
        c.drawPath(path,p);
        return defaultAvatar;
    }
    
//    public void createDefaultAvatar(MegaUser contact,ImageView avatarImage) {
//        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(defaultAvatar);
//        Paint p = new Paint();
//        p.setAntiAlias(true);
//        String color = megaApi.getUserAvatarColor(contact);
//        if (color != null) {
//            p.setColor(Color.parseColor(color));
//        } else {
//            p.setColor(ContextCompat.getColor(context,R.color.lollipop_primary_color));
//        }
//        p.setStyle(Paint.Style.FILL);
//        Path path = ThumbnailUtilsLollipop.getRoundedRect(0,0,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,RX,RY,true,true,false,false);
//        c.drawPath(path,p);
//        avatarImage.setImageBitmap(defaultAvatar);
//
//        Display display = context.getWindowManager().getDefaultDisplay();
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        display.getMetrics(outMetrics);
//        float density = context.getResources().getDisplayMetrics().density;
//
//        String fullName = contact.getFullName();
//
//        int avatarTextSize = Util.getAvatarTextSize(density);
//
//        String firstLetter = fullName.charAt(0) + "";
//        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
//        holder.contactInitialLetter.setText(firstLetter);
//        holder.contactInitialLetter.setTextColor(Color.WHITE);
//        holder.contactInitialLetter.setVisibility(View.VISIBLE);
//
//        if (adapterType == ITEM_VIEW_TYPE_LIST || adapterType == ITEM_VIEW_TYPE_LIST_ADD_CONTACT || adapterType == ITEM_VIEW_TYPE_LIST_GROUP_CHAT) {
//            holder.contactInitialLetter.setTextSize(24);
//        } else if (adapterType == ITEM_VIEW_TYPE_GRID) {
//            holder.contactInitialLetter.setTextSize(64);
//        }
//    }
    
    private List<MegaUser> subLastContacts(List<MegaUser> lastContacts) {
//        lastContacts.addAll(lastContacts);
//        lastContacts.addAll(lastContacts);
//        lastContacts.addAll(lastContacts);
        if (lastContacts.size() > MAX_CONTACTS) {
            return lastContacts.subList(0,MAX_CONTACTS);
        }
//        lastContacts = lastContacts.subList(0,6);
        return lastContacts;
    }
    
    protected static class ViewHolder extends RecyclerView.ViewHolder {
        
        private ImageView avatarImage;
        
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
