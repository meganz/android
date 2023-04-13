package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_ADD_CONTACTS;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.getCircleBitmap;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.AddContactActivity;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;

public class MegaAddContactsAdapter extends RecyclerView.Adapter<MegaAddContactsAdapter.ViewHolderChips> implements View.OnClickListener {

    private int positionClicked;
    ArrayList<MegaContactAdapter> contacts;
    private MegaApiAndroid megaApi;
    private Context context;

    public MegaAddContactsAdapter(Context _context, ArrayList<MegaContactAdapter> _contacts) {
        this.contacts = _contacts;
        this.context = _context;
        this.positionClicked = -1;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
    }

    public static class ViewHolderChips extends RecyclerView.ViewHolder {
        public ViewHolderChips(View itemView) {
            super(itemView);
        }

        EmojiTextView textViewName;
        ImageView deleteIcon;
        RoundedImageView avatar;
        RelativeLayout itemLayout;

    }

    ViewHolderChips holder = null;

    @Override
    public MegaAddContactsAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = v.findViewById(R.id.item_layout_chip);
        holder.itemLayout.setOnClickListener(this);

        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidthEmojis(dp2px(MAX_WIDTH_ADD_CONTACTS, outMetrics));

        holder.avatar = v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = v.findViewById(R.id.delete_icon_chip);
        holder.itemLayout.setTag(holder);
        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(MegaAddContactsAdapter.ViewHolderChips holder, int position) {
        Timber.d("onBindViewHolderList");

        MegaContactAdapter contact = (MegaContactAdapter) getItem(position);

        String[] s;

        if (contact.getFullName() != null) {
            if (contact.getMegaUser() == null && contact.getMegaContactDB() == null) {
                s = contact.getFullName().split("[@._]");
                if (s != null && s.length > 0) {
                    holder.textViewName.setText(s[0]);
                } else {
                    holder.textViewName.setText(contact.getFullName());
                }
            } else {
                s = contact.getFullName().split(" ");
                if (s != null && s.length > 0) {
                    holder.textViewName.setText(s[0]);
                } else {
                    holder.textViewName.setText(contact.getFullName());
                }
            }
        }
        holder.avatar.setImageBitmap(setUserAvatar(contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public void onClick(View view) {
        Timber.d("onClick");

        ViewHolderChips holder = (ViewHolderChips) view.getTag();
        if (holder != null) {
            int currentPosition = holder.getLayoutPosition();
            Timber.d("Current position: %s", currentPosition);

            if (currentPosition < 0) {
                Timber.e("Current position error - not valid value");
                return;
            }
            switch (view.getId()) {
                case R.id.item_layout_chip: {
                    ((AddContactActivity) context).deleteContact(currentPosition);
                    break;
                }
            }
        } else {
            Timber.e("Error. Holder is Null");
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPositionClicked() {
        return positionClicked;
    }

    public void setPositionClicked(int p) {
        Timber.d("Position: %s", p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setContacts(ArrayList<MegaContactAdapter> contacts) {
        Timber.d("setContacts");
        this.contacts = contacts;

        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        Timber.d("Position: %s", position);
        return contacts.get(position);
    }

    private Bitmap setUserAvatar(MegaContactAdapter contact) {
        Timber.d("setUserAvatar");

        File avatar = null;
        String mail;

        if (contact.getMegaUser() != null && contact.getMegaUser().getEmail() != null) {
            mail = contact.getMegaUser().getEmail();
        } else if (contact.getMegaContactDB() != null && contact.getMegaContactDB().getEmail() != null) {
            mail = contact.getMegaContactDB().getEmail();
        } else {
            mail = contact.getFullName();
        }

        int color = getColorAvatar(contact.getMegaUser());

        if (contact.getMegaUser() == null && contact.getMegaContactDB() == null) {
            return getDefaultAvatar(color, contact.getFullName(), AVATAR_SIZE, true);
        }

        /*Avatar*/
        avatar = buildAvatarFile(context, mail + ".jpg");
        Bitmap bitmap = null;
        if (isFileAvailable(avatar) && avatar.length() > 0) {
            BitmapFactory.Options bOpts = new BitmapFactory.Options();
            bOpts.inPurgeable = true;
            bOpts.inInputShareable = true;
            bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
            if (bitmap != null) {
                return getCircleBitmap(bitmap);
            }
        }

        /*Default Avatar*/
        String fullName;
        if (contact.getFullName() != null) {
            fullName = contact.getFullName();
        } else {
            fullName = mail;
        }
        return getDefaultAvatar(color, fullName, AVATAR_SIZE, true);
    }


}
