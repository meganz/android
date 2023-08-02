package mega.privacy.android.app.main.adapters;

import static mega.privacy.android.app.utils.AvatarUtil.getAvatarShareContact;
import static mega.privacy.android.app.utils.Constants.MAX_WIDTH_ADD_CONTACTS;
import static mega.privacy.android.app.utils.Util.dp2px;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.AddContactActivity;
import mega.privacy.android.app.main.ShareContactInfo;
import nz.mega.sdk.MegaApiAndroid;
import timber.log.Timber;


public class ShareContactsAdapter extends RecyclerView.Adapter<ShareContactsAdapter.ViewHolderChips> implements View.OnClickListener {

    private int positionClicked;
    ArrayList<ShareContactInfo> contacts;
    private MegaApiAndroid megaApi;
    private Context context;

    private boolean isContactVerificationOn;

    public ShareContactsAdapter(Context _context, ArrayList<ShareContactInfo> _contacts, boolean isContactVerificationOn) {
        this.contacts = _contacts;
        this.context = _context;
        this.positionClicked = -1;
        this.isContactVerificationOn = isContactVerificationOn;
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
        ConstraintLayout itemLayout;

        ImageView verifiedIcon;
    }

    ViewHolderChips holder = null;

    @Override
    public ShareContactsAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = (ConstraintLayout) v.findViewById(R.id.item_layout_chip);
        holder.itemLayout.setOnClickListener(this);

        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidthEmojis(dp2px(MAX_WIDTH_ADD_CONTACTS, outMetrics));
        holder.avatar = (RoundedImageView) v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = (ImageView) v.findViewById(R.id.delete_icon_chip);
        holder.itemLayout.setTag(holder);
        holder.verifiedIcon = (ImageView) v.findViewById(R.id.verified_icon);
        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(ShareContactsAdapter.ViewHolderChips holder, int position) {
        Timber.d("Position: %s", position);
        holder.verifiedIcon.setVisibility(View.GONE);
        ShareContactInfo contact = (ShareContactInfo) getItem(position);
        String[] s;
        if (contact.isPhoneContact()) {
            if (contact.getPhoneContactInfo().getName() != null) {
                s = contact.getPhoneContactInfo().getName().split(" ");
                if (s != null && s.length > 0) {
                    holder.textViewName.setText(s[0]);
                } else {
                    holder.textViewName.setText(contact.getPhoneContactInfo().getName());
                }
            } else {
                s = contact.getPhoneContactInfo().getEmail().split("[@._]");
                if (s != null && s.length > 0) {
                    holder.textViewName.setText(s[0]);
                } else {
                    holder.textViewName.setText(contact.getPhoneContactInfo().getEmail());
                }
            }
        } else if (contact.isMegaContact()) {
            if (contact.getMegaContactAdapter().getFullName() != null) {
                if (contact.getMegaContactAdapter().getMegaUser() == null && contact.getMegaContactAdapter().getMegaContactDB() == null) {
                    s = contact.getMegaContactAdapter().getFullName().split("[@._]");
                    if (s != null && s.length > 0) {
                        holder.textViewName.setText(s[0]);
                    } else {
                        holder.textViewName.setText(contact.getMegaContactAdapter().getFullName());
                    }
                } else {
                    s = contact.getMegaContactAdapter().getFullName().split(" ");
                    if (s != null && s.length > 0) {
                        holder.textViewName.setText(s[0]);
                    } else {
                        holder.textViewName.setText(contact.getMegaContactAdapter().getFullName());
                    }
                   holder.verifiedIcon.setVisibility(
                                megaApi.areCredentialsVerified(contact.getMegaContactAdapter().getMegaUser()) && isContactVerificationOn ? View.VISIBLE : View.GONE);
                }
            }
        } else {
            s = contact.getMail().split("[@._]");
            if (s != null && s.length > 0) {
                holder.textViewName.setText(s[0]);
            } else {
                holder.textViewName.setText(contact.getMail());
            }
        }

        holder.avatar.setImageBitmap(getAvatarShareContact(context, contact));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    @Override
    public void onClick(View view) {
        Timber.d("onClick");

        ShareContactsAdapter.ViewHolderChips holder = (ShareContactsAdapter.ViewHolderChips) view.getTag();
        if (holder != null) {
            int currentPosition = holder.getLayoutPosition();
            Timber.d("Current position: %s", currentPosition);

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
        Timber.d("Position: %s", p);
        positionClicked = p;
        notifyDataSetChanged();
    }

    public void setContacts(ArrayList<ShareContactInfo> contacts) {
        Timber.d("setContacts");
        this.contacts = contacts;

        notifyDataSetChanged();
    }

    public Object getItem(int position) {
        Timber.d("Position: %s", position);
        return contacts.get(position);
    }
}
