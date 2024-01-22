package mega.privacy.android.app.main.megachat.chatAdapters;

import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getSpecificAvatarColor;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.Constants.AVATAR_GROUP_CHAT_COLOR;
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

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.main.listeners.ChatUserAvatarListener;
import mega.privacy.android.app.main.megachat.ChatExplorerFragment;
import mega.privacy.android.app.main.megachat.ChatExplorerListItem;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;


public class MegaChipChatExplorerAdapter extends RecyclerView.Adapter<MegaChipChatExplorerAdapter.ViewHolderChips> implements View.OnClickListener {

    private ArrayList<ChatExplorerListItem> items;
    private MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    private Context context;
    private Object fragment;

    public MegaChipChatExplorerAdapter(Context _context, Object _fragment, ArrayList<ChatExplorerListItem> _items) {
        this.items = _items;
        this.context = _context;
        this.fragment = _fragment;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
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

        String email;

        public String getEmail() {
            return email;
        }

        public void setAvatar(Bitmap avatar) {
            this.avatar.setImageBitmap(avatar);
        }

    }

    ViewHolderChips holder = null;

    @Override
    public MegaChipChatExplorerAdapter.ViewHolderChips onCreateViewHolder(ViewGroup parent, int viewType) {
        Timber.d("onCreateViewHolder");

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip_avatar, parent, false);

        holder = new ViewHolderChips(v);
        holder.itemLayout = v.findViewById(R.id.item_layout_chip);
        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidthEmojis(dp2px(MAX_WIDTH_ADD_CONTACTS, outMetrics));
        holder.avatar = v.findViewById(R.id.rounded_avatar);
        holder.deleteIcon = v.findViewById(R.id.delete_icon_chip);
        holder.deleteIcon.setOnClickListener(this);
        holder.deleteIcon.setTag(holder);
        v.setTag(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(MegaChipChatExplorerAdapter.ViewHolderChips holder, int position) {
        Timber.d("onBindViewHolderList");

        ChatExplorerListItem item = getItem(position);
        if (item.getChat() != null && item.getChat().isGroup()) {
            holder.textViewName.setText(item.getTitle());
        } else {
            String name;
            if (item.getContact() != null) {
                String fullName = item.getContact().getFullName();
                if (fullName != null) {
                    String[] s = fullName.split(" ");
                    if (s.length > 0) {
                        name = s[0];
                    } else {
                        name = getName(item.getTitle());
                    }
                } else {
                    name = getName(item.getTitle());
                }
            } else {
                name = getName(item.getTitle());
            }
            holder.textViewName.setText(name);
        }
        setUserAvatar(holder, item);
    }

    /**
     * Get name
     *
     * @param title chat title
     * @return Name
     */
    private String getName(String title) {
        String name = " ";
        if (title != null) {
            String[] splitTitle = title.split(" ");
            if (splitTitle.length > 0) name = splitTitle[0];
            else name = title;
        }

        return name;
    }

    @Override
    public int getItemCount() {
        if (items == null) return 0;

        return items.size();
    }

    @Override
    public void onClick(View view) {
        Timber.d("onClick");

        MegaChipChatExplorerAdapter.ViewHolderChips holder = (MegaChipChatExplorerAdapter.ViewHolderChips) view.getTag();
        if (holder != null) {
            int currentPosition = holder.getLayoutPosition();
            Timber.d("Current position: %s", currentPosition);

            if (currentPosition < 0) {
                Timber.w("Current position error - not valid value");
                return;
            }
            if (view.getId() == R.id.delete_icon_chip) {
                ((ChatExplorerFragment) fragment).deleteItemPosition(currentPosition);
            }
        } else {
            Timber.w("Error. Holder is Null");
        }
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void setItems(ArrayList<ChatExplorerListItem> items) {
        Timber.d("setContacts");
        this.items = items;
        notifyDataSetChanged();
    }

    public ChatExplorerListItem getItem(int position) {
        Timber.d("position: %s", position);
        return items.get(position);
    }

    public ArrayList<ChatExplorerListItem> getItems() {
        return items;
    }

    public void setUserAvatar(ViewHolderChips holder, ChatExplorerListItem item) {
        Timber.d("setUserAvatar");

        if (item.getChat() != null && item.getChat().isGroup()) {
            holder.avatar.setImageBitmap(getDefaultAvatar(getSpecificAvatarColor(AVATAR_GROUP_CHAT_COLOR), item.getTitle(), AVATAR_SIZE, true));
        } else {
            MegaUser user = null;
            if (item.getContact() != null && item.getContact().getMegaUser() != null) {
                user = item.getContact().getMegaUser();
            }
            holder.avatar.setImageBitmap(getDefaultAvatar(getColorAvatar(user), item.getTitle(), AVATAR_SIZE, true));

            ChatUserAvatarListener listener = new ChatUserAvatarListener(context, holder);
            File avatar = null;

            long handle = -1;
            if (item.getChat() != null) {
                holder.email = megaChatApi.getContactEmail(item.getChat().getPeerHandle());
            } else if (item.getContact() != null && item.getContact().getMegaUser() != null) {
                holder.email = item.getContact().getMegaUser().getEmail();
            }

            if (item.getContact() != null && item.getContact().getMegaUser() != null) {
                handle = item.getContact().getMegaUser().getHandle();
            }
            String userHandle = MegaApiAndroid.userHandleToBase64(handle);

            if (holder.email == null) {
                avatar = buildAvatarFile(userHandle + ".jpg");
            } else {
                avatar = buildAvatarFile(holder.email + ".jpg");
            }

            Bitmap bitmap = null;
            if (isFileAvailable(avatar)) {
                if (avatar.length() > 0) {
                    BitmapFactory.Options bOpts = new BitmapFactory.Options();
                    bOpts.inPurgeable = true;
                    bOpts.inInputShareable = true;
                    bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                    if (bitmap == null) {
                        avatar.delete();

                        if (megaApi == null) {
                            Timber.w("megaApi is Null in Offline mode");
                            return;
                        }

                        megaApi.getUserAvatar(holder.email, buildAvatarFile(holder.email + ".jpg").getAbsolutePath(), listener);
                    } else {
                        holder.avatar.setImageBitmap(getCircleBitmap(bitmap));
                    }
                } else {

                    if (megaApi == null) {
                        Timber.w("megaApi is Null in Offline mode");
                        return;
                    }

                    megaApi.getUserAvatar(holder.email, buildAvatarFile(holder.email + ".jpg").getAbsolutePath(), listener);
                }
            } else {

                if (megaApi == null) {
                    Timber.w("megaApi is Null in Offline mode");
                    return;
                }

                megaApi.getUserAvatar(holder.email, buildAvatarFile(holder.email + ".jpg").getAbsolutePath(), listener);
            }
        }
    }

}
