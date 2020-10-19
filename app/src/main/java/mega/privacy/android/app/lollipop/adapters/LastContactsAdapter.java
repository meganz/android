package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.recyclerview.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.AvatarUtil.*;
import static mega.privacy.android.app.utils.Constants.*;

public class LastContactsAdapter extends RecyclerView.Adapter<LastContactsAdapter.ViewHolder> {
    
    /**
     * The last contacts grid has maxium 3 columns.
     */
    public static final int MAX_COLUMN = 3;
    
    /**
     * The last contacts grid displays 6 last contacts at most.
     */
    private static final int MAX_CONTACTS = 6;
    
    private Activity context;
    
    private MegaApiAndroid megaApi;
    
    private List<MegaUser> lastContacts;
    
    private DatabaseHandler dbH;
    
    public LastContactsAdapter(final Activity context,List<MegaUser> data) {
        this.context = context;
        this.lastContacts = subLastContacts(data);
        this.lastContacts = reOrder(this.lastContacts);
        
        if (megaApi == null) {
            megaApi = ((MegaApplication)context.getApplication()).getMegaApi();
        }
        dbH = DatabaseHandler.getDbHandler(context);
    }
    
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent,int viewType) {
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
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
                    toChat(contact);
                }
            }
        });
        holder.avatarImage.setTag(holder);
        return holder;
    }
    
    private void toChat(MegaUser contact) {
        MegaChatApiAndroid megaChatApi = ((MegaApplication)context.getApplication()).getMegaChatApi();
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(contact.getHandle());
        if (chat == null) {
            toContactInfo(contact);
        } else {
            Intent intentOpenChat = new Intent(context,ChatActivityLollipop.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra("CHAT_ID",chat.getChatId());
            intentOpenChat.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intentOpenChat);
        }
    }
    
    private void toContactInfo(MegaUser contact) {
        Intent i = new Intent(context,ContactInfoActivityLollipop.class);
        i.putExtra(NAME, contact.getEmail());
        context.startActivity(i);
    }
    
    @Override
    public void onBindViewHolder(ViewHolder holder,int position) {
        MegaUser contact = lastContacts.get(position);
        //Placeholder.
        if (contact == null) {
            return;
        }
        setDefaultAvatar(contact,holder);
        
        //Set real avatar.
        String email = contact.getEmail();
        holder.contactMail = email;
        UserAvatarListener listener = new UserAvatarListener(context,holder);
        File avatar = buildAvatarFile(context, email + ".jpg");
        Bitmap bitmap;
        if (isFileAvailable(avatar)) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(),bOpts);
                if (bitmap == null) {
                    avatar.delete();
                    megaApi.getUserAvatar(contact,buildAvatarFile(context,email + ".jpg").getAbsolutePath(),listener);
                } else {
                    holder.avatarImage.setImageBitmap(bitmap);
                }
            } else {
                megaApi.getUserAvatar(contact,buildAvatarFile(context,email + ".jpg").getAbsolutePath(),listener);
            }
        } else {
            megaApi.getUserAvatar(contact,buildAvatarFile(context,email + ".jpg").getAbsolutePath(),listener);
        }
        
    }

    private void setDefaultAvatar(MegaUser contact,ViewHolder holder) {
        String fullName = getMegaUserNameDB(contact);
        Bitmap bitmap = getDefaultAvatar(getColorAvatar(contact), fullName, AVATAR_SIZE, true);
        holder.avatarImage.setImageBitmap(bitmap);
    }

    
    @Override
    public int getItemCount() {
        return lastContacts.size();
    }
    
    private List<MegaUser> subLastContacts(List<MegaUser> lastContacts) {
        if (lastContacts.size() > MAX_CONTACTS) {
            return lastContacts.subList(0,MAX_CONTACTS);
        }
        return lastContacts;
    }
    
    /**
     * Calculate the new position of each element.
     * To adjust the top to bottom, right to left layout.
     * <p>
     * In this case, with max 6 elements:
     * 0 1 2 >>> 4 2 0
     * 3 4 5     5 3 1
     *
     * @param lastContacts Original last contacts list.
     * @return New last contacts list with new element's position.
     */
    private List<MegaUser> reOrder(List<MegaUser> lastContacts) {
        int col = MAX_COLUMN;
        int row = getRowCount(MAX_CONTACTS,MAX_COLUMN);
        MegaUser[][] matrix = new MegaUser[row][col];
        for (int i = 0, y = row * col - 1;i < lastContacts.size();i++,y--) {
            int r = i % (col - (col - row));
            int c = y / row;
            matrix[r][c] = lastContacts.get(i);
        }
        List<MegaUser> list = new ArrayList<>(lastContacts.size());
        for (int i = 0;i < row;i++) {
            list.addAll(Arrays.asList(matrix[i]));
        }
        return list;
    }
    
    private int getRowCount(int total,int colCount) {
        if (total % colCount == 0) {
            return total / colCount;
        }
        return (total / colCount) + 1;
    }
    
    public static class ViewHolder extends MegaContactsLollipopAdapter.ViewHolderContacts {
        
        public ImageView avatarImage;
        
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
