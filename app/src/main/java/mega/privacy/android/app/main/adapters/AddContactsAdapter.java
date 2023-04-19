package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor;
import static mega.privacy.android.app.utils.Constants.AVATAR_PHONE_COLOR;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_ADD_CONTACTS;
import static mega.privacy.android.app.utils.Util.dp2px;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.AddContactActivity;
import mega.privacy.android.app.main.PhoneContactInfo;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;


public class AddContactsAdapter extends RecyclerView.Adapter<AddContactsAdapter.ViewHolderChips> implements View.OnClickListener {

    private int positionClicked;
    ArrayList<PhoneContactInfo> contacts;
    private MegaApiAndroid megaApi;
    private Context context;

    public AddContactsAdapter(Context _context, ArrayList<PhoneContactInfo> _contacts) {
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
    public AddContactsAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.item_layout_chip);
        holder.itemLayout.setOnClickListener(this);

        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidthEmojis(dp2px(MAX_WIDTH_ADD_CONTACTS, outMetrics));
        holder.textViewName.setTypeEllipsize(TextUtils.TruncateAt.MIDDLE);
        holder.avatar = (RoundedImageView) v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = (ImageView) v.findViewById(R.id.delete_icon_chip);

        holder.itemLayout.setTag(holder);

        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(AddContactsAdapter.ViewHolderChips holder, int position) {
        Timber.d("onBindViewHolderList");

        PhoneContactInfo contact = (PhoneContactInfo) getItem(position);
        String[] s;
        if (contact.getName() != null) {
            s = contact.getName().split(" ");
            if (s != null && s.length > 0) {
                holder.textViewName.setText(s[0]);
            } else {
                holder.textViewName.setText(contact.getName());
            }
        } else {
            s = contact.getEmail().split("[@._]");
            if (s != null && s.length > 0) {
                holder.textViewName.setText(s[0]);
            } else {
                holder.textViewName.setText(contact.getEmail());
            }
        }
        holder.avatar.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_PHONE_COLOR), holder.textViewName.getText().toString(), AVATAR_SIZE, true));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public void onClick(View view) {
        Timber.d("onClick");

        AddContactsAdapter.ViewHolderChips holder = (AddContactsAdapter.ViewHolderChips) view.getTag();
        if (holder != null) {
            int currentPosition = holder.getLayoutPosition();
            Timber.d("onClick -> Current position: %s", currentPosition);

            if (currentPosition < 0) {
                Timber.e("Current position error - not valid value");
                return;
            }
            if (view.getId() == R.id.item_layout_chip) {
                ((AddContactActivity) context).deleteContact(currentPosition);
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
        Timber.d("Position clicked: %s", p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setContacts(ArrayList<PhoneContactInfo> contacts) {
        Timber.d("setContacts");
        this.contacts = contacts;

        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        Timber.d("Position: %s", position);
        return contacts.get(position);
    }
}
