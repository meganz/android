package mega.privacy.android.app.fragments.managerFragments.cu;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.repo.MegaNodeRepo;
import nz.mega.sdk.MegaApiAndroid;

class CuViewModelFactory implements ViewModelProvider.Factory {
    private final MegaApiAndroid mMegaApi;
    private final DatabaseHandler mDbHandler;
    private final MegaNodeRepo mRepo;
    private final Context mContext;
    private final int mType;

    CuViewModelFactory(MegaApiAndroid megaApi, DatabaseHandler dbHandler, MegaNodeRepo repo,
            Context context, int type) {
        mMegaApi = megaApi;
        mDbHandler = dbHandler;
        mRepo = repo;
        mContext = context;
        mType = type;
    }

    @SuppressWarnings("unchecked")
    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(CuViewModel.class)) {
            return (T) new CuViewModel(mMegaApi, mDbHandler, mRepo, mContext, mType);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
