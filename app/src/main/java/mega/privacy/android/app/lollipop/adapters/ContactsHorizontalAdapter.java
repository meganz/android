package mega.privacy.android.app.lollipop.adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
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
import java.util.List;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.listeners.UserAvatarListener;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.TL;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;

public class ContactsHorizontalAdapter extends RecyclerView.Adapter<ContactsHorizontalAdapter.ContactViewHolder> implements View.OnClickListener {

    private Activity context;

    private List<MegaUser> contacts;

    private ContactViewHolder holder;

    private DatabaseHandler dbH;

    private MegaApiAndroid megaApi;

    public ContactsHorizontalAdapter(Activity context,List<MegaUser> data) {
        this.context = context;
        this.contacts = data;
        if (megaApi == null) {
            megaApi = ((MegaApplication)context.getApplication()).getMegaApi();
        }
        dbH = DatabaseHandler.getDbHandler(context);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent,int viewType) {
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_add_avatar,parent,false);

        holder = new ContactViewHolder(v);
        holder.itemLayout = v.findViewById(R.id.chip_layout);
        holder.contactInitialLetter = v.findViewById(R.id.contact_list_initial_letter);
        holder.textViewName = v.findViewById(R.id.name_chip);
        holder.textViewName.setMaxWidth(Util.px2dp(60,outMetrics));
        holder.avatar = v.findViewById(R.id.add_rounded_avatar);
        holder.addIcon = v.findViewById(R.id.add_icon_chip);
        holder.addIcon.setOnClickListener(this);
        holder.addIcon.setTag(holder);
        v.setTag(holder);
        return holder;
    }

    @Override
    public void onClick(View v) {
        TL.log(this, "@#@", "add it!");
    }

    @Override
    public void onBindViewHolder(@NonNull final ContactViewHolder holder,int position) {
        final MegaUser megaUser = getItem(position);
        megaApi.getUserAttribute(megaUser,1,new MegaRequestListenerInterface() {

            @Override
            public void onRequestStart(MegaApiJava api,MegaRequest request) {

            }

            @Override
            public void onRequestUpdate(MegaApiJava api,MegaRequest request) {

            }

            @Override
            public void onRequestFinish(MegaApiJava api,MegaRequest request,MegaError e) {
                if (e.getErrorCode() == MegaError.API_OK) {

                    if (request.getParamType() == MegaApiJava.USER_ATTR_FIRSTNAME) {
                        log("(ManagerActivityLollipop(1)request.getText(): " + request.getText() + " -- " + request.getEmail());
                        holder.textViewName.setText(request.getText());
                        if(holder.contactInitialLetter.getVisibility() != View.GONE) {
                            setDefaultAvatar(megaUser, holder);
                        }
                    }
                }
            }

            @Override
            public void onRequestTemporaryError(MegaApiJava api,MegaRequest request,MegaError e) {

            }
        });

        String email = megaUser.getEmail();
        holder.contactMail = email;
        UserAvatarListener listener = new UserAvatarListener(context,holder);
        File avatar;
        if (context.getExternalCacheDir() != null) {
            avatar = new File(context.getExternalCacheDir().getAbsolutePath(),email + ".jpg");
        } else {
            avatar = new File(context.getCacheDir().getAbsolutePath(),email + ".jpg");
        }
        Bitmap bitmap;
        if (avatar.exists()) {
            if (avatar.length() > 0) {
                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                bOpts.inPurgeable = true;
                bOpts.inInputShareable = true;
                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(),bOpts);
                if (bitmap == null) {
                    avatar.delete();
                    if (context.getExternalCacheDir() != null) {
                        megaApi.getUserAvatar(megaUser,context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg",listener);
                    } else {
                        megaApi.getUserAvatar(megaUser,context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg",listener);
                    }
                } else {
                    holder.contactInitialLetter.setVisibility(View.GONE);
                    holder.avatar.setImageBitmap(bitmap);
                }
            } else {
                if (context.getExternalCacheDir() != null) {
                    megaApi.getUserAvatar(megaUser,context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg",listener);
                } else {
                    megaApi.getUserAvatar(megaUser,context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg",listener);
                }
            }
        } else {
            if (context.getExternalCacheDir() != null) {
                megaApi.getUserAvatar(megaUser,context.getExternalCacheDir().getAbsolutePath() + "/" + email + ".jpg",listener);
            } else {
                megaApi.getUserAvatar(megaUser,context.getCacheDir().getAbsolutePath() + "/" + email + ".jpg",listener);
            }
        }
    }

    public void setDefaultAvatar(MegaUser contact,ContactViewHolder holder) {
        //Draw circle with color filled.
        drawCircle(contact,holder);
        //Set the first letter.
        displayFirstLetter(contact,holder);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public MegaUser getItem(int position) {
        log("getItem");
        return contacts.get(position);
    }

    public static class ContactViewHolder extends MegaContactsLollipopAdapter.ViewHolderContacts {

        TextView textViewName;

        ImageView addIcon;

        public RoundedImageView avatar;

        RelativeLayout itemLayout;

        public ContactViewHolder(View itemView) {
            super(itemView);
        }
    }

    private void drawCircle(MegaUser contact,ContactViewHolder holder) {
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
        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight()) {
            radius = defaultAvatar.getWidth() / 2;
        } else {
            radius = defaultAvatar.getHeight() / 2;
        }
        c.drawCircle(defaultAvatar.getWidth() / 2,defaultAvatar.getHeight() / 2,radius,p);
        holder.avatar.setImageBitmap(defaultAvatar);
    }

    private void displayFirstLetter(MegaUser contact,ContactViewHolder holder) {
        String firstLetter = getFirstLetter(holder);
        firstLetter = firstLetter.toUpperCase(Locale.getDefault());
        holder.contactInitialLetter.setText(firstLetter);
        holder.contactInitialLetter.setTextColor(Color.WHITE);
        holder.contactInitialLetter.setVisibility(View.VISIBLE);
        holder.contactInitialLetter.setTextSize(12);
    }

    private String getFirstLetter(ContactViewHolder holder) {
        return String.valueOf(holder.textViewName.getText().charAt(0));
    }

    private static void log(String log) {
        Util.log("ContactsHorizontalAdapter",log);
    }
}
