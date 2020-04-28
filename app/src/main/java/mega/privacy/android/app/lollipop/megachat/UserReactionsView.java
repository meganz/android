package mega.privacy.android.app.lollipop.megachat;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import static mega.privacy.android.app.utils.LogUtil.*;

public class UserReactionsView extends ListView implements View.OnClickListener {
//    protected EmojiArrayAdapter contactReactionsAdapter;
    public UserReactionsView(final Context context) {
        super(context);
        setClipToPadding(false);
        setVerticalScrollBarEnabled(true);
    }

    public UserReactionsView init() {
//        contactReactionsAdapter = new EmojiArrayAdapter(getContext(), category.getEmojis(), variantManager, onEmojiClickListener, onEmojiLongClickListener);
//        setAdapter(contactReactionsAdapter);

        return this;
    }

    @Override
    public void onClick(View view) {

    }
}
