package nz.mega.android.authenticator;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import nz.mega.android.LoginActivity;
import nz.mega.android.utils.Util;
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;


public class MegaAuthenticator extends AbstractAccountAuthenticator{

	private final Context context;
	
	public MegaAuthenticator(Context context) {
		super(context);
		this.context = context;
	}

	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
		log("addAccount");
		
		log("accountType = " + accountType + "____ authtokentype = " + authTokenType);

        final Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
	}

	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
		return null;
	}

	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		return null;
	}

	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse arg0, Account arg1,
			String arg2, Bundle arg3) throws NetworkErrorException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAuthTokenLabel(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
		final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
	}

	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
		return null;
	}
	
	public static void log(String log){
		Util.log("MegaAuthenticator", log);
	}

}
