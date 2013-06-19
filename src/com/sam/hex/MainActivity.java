package com.sam.hex;

import java.io.UnsupportedEncodingException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;

import com.android.vending.billing.util.IabResult;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateLoadedListener;
import com.hex.core.Game;
import com.sam.hex.fragment.GameFragment;
import com.sam.hex.fragment.GameSelectionFragment;
import com.sam.hex.fragment.HistoryFragment;
import com.sam.hex.fragment.InstructionsFragment;
import com.sam.hex.fragment.MainFragment;
import com.sam.hex.fragment.OnlineSelectionFragment;

/**
 * @author Will Harmon
 **/
public class MainActivity extends NetActivity implements OnStateLoadedListener {
    public static int PLAY_TIME_STATE = 0;
    public static int GAMES_PLAYED_STATE = 1;
    public static int GAMES_WON_STATE = 2;

    // Play variables
    private boolean mIsSignedIn = false;
    private boolean mOpenAchievements = false;
    private boolean mOpenOnlineSelectionFragment = false;

    // Donate variables
    private boolean mIabSetup;

    // Fragments
    private MainFragment mMainFragment;
    private GameFragment mGameFragment;
    private GameSelectionFragment mGameSelectionFragment;
    private HistoryFragment mHistoryFragment;
    private InstructionsFragment mInstructionsFragment;
    private OnlineSelectionFragment mOnlineSelectionFragment;
    private Fragment mActiveFragment;

    public MainActivity() {
        super(CLIENT_GAMES | CLIENT_APPSTATE);
    }

    @Override
    public void onStateConflict(int stateKey, String ver, byte[] localData, byte[] serverData) {
        byte[] resolvedData = serverData;
        try {
            if(stateKey == PLAY_TIME_STATE) {
                resolvedData = String.valueOf(Math.max(Long.parseLong(new String(localData, "UTF-8")), Long.parseLong(new String(serverData, "UTF-8"))))
                        .getBytes();
            }
            else if(stateKey == GAMES_PLAYED_STATE) {
                resolvedData = String.valueOf(Math.max(Long.parseLong(new String(localData, "UTF-8")), Long.parseLong(new String(serverData, "UTF-8"))))
                        .getBytes();
            }
            else if(stateKey == GAMES_WON_STATE) {
                resolvedData = String.valueOf(Math.max(Long.parseLong(new String(localData, "UTF-8")), Long.parseLong(new String(serverData, "UTF-8"))))
                        .getBytes();
            }
        }
        catch(UnsupportedEncodingException e) {}

        getAppStateClient().resolveState(this, stateKey, ver, resolvedData);
    }

    @Override
    public void onStateLoaded(int statusCode, int stateKey, byte[] buffer) {
        if(statusCode == AppStateClient.STATUS_OK) {
            try {
                if(stateKey == PLAY_TIME_STATE) {
                    String s = new String(buffer, "UTF-8");
                    Stats.setTimePlayed(this, Long.parseLong(s));
                }
                else if(stateKey == GAMES_PLAYED_STATE) {
                    String s = new String(buffer, "UTF-8");
                    Stats.setGamesPlayed(this, Long.parseLong(s));
                }
                else if(stateKey == GAMES_WON_STATE) {
                    String s = new String(buffer, "UTF-8");
                    Stats.setGamesWon(this, Long.parseLong(s));
                }
                mMainFragment.setSignedIn(mIsSignedIn);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // mHexRealTimeMessageReceivedListener = new
        // HexRealTimeMessageReceivedListener();
        // mHexRoomStatusUpdateListener = new HexRoomStatusUpdateListener();
        // mHexRoomUpdateListener = new HexRoomUpdateListener(this);

        mMainFragment = new MainFragment();
        mMainFragment.setInitialRotation(-120f);
        mMainFragment.setInitialSpin(50f);
        swapFragment(mMainFragment);

        popupRatingDialog();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(mActiveFragment == mHistoryFragment) {
                if(mHistoryFragment.goUp()) return true;
            }

            if(mActiveFragment != mMainFragment) {
                returnHome();
            }
            else {
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void returnHome() {
        swapFragment(mMainFragment);
    }

    @Override
    public void onSignInSucceeded() {
        super.onSignInSucceeded();
        mIsSignedIn = true;
        getAppStateClient().loadState(this, PLAY_TIME_STATE);
        getAppStateClient().loadState(this, GAMES_PLAYED_STATE);
        getAppStateClient().loadState(this, GAMES_WON_STATE);
        mMainFragment.setSignedIn(mIsSignedIn);

        if(mOpenAchievements) {
            mOpenAchievements = false;
            startActivityForResult(getGamesClient().getAchievementsIntent(), RC_ACHIEVEMENTS);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        if(mOpenOnlineSelectionFragment) {
            mOpenOnlineSelectionFragment = false;
            setOnlineSelectionFragment(new OnlineSelectionFragment());
            swapFragment(this.getOnlineSelectionFragment());
        }
    }

    @Override
    public void onSignInFailed() {
        super.onSignInFailed();
        mIsSignedIn = false;
        mMainFragment.setSignedIn(mIsSignedIn);
    }

    @Override
    protected void dealWithIabSetupSuccess() {
        setIabSetup(true);
    }

    @Override
    protected void dealWithIabSetupFailure() {
        setIabSetup(false);
    }

    @Override
    protected void dealWithPurchaseSuccess(IabResult result, String sku) {
        int amount = 0;
        if(sku.equals(ITEM_SKU_BASIC)) {
            amount = 1;
        }
        else if(sku.equals(ITEM_SKU_INTERMEDIATE)) {
            amount = 3;
        }
        else if(sku.equals(ITEM_SKU_ADVANCED)) {
            amount = 5;
        }
        Stats.incrementDonationAmount(this, amount);
    }

    @Override
    protected void dealWithPurchaseFailed(IabResult result) {}

    public void swapFragment(Fragment newFragment) {
        getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(R.id.content, newFragment)
                .commit();
        mActiveFragment = newFragment;
    }

    public MainFragment getMainFragment() {
        return mMainFragment;
    }

    public void setMainFragment(MainFragment mainFragment) {
        this.mMainFragment = mainFragment;
    }

    public GameFragment getGameFragment() {
        return mGameFragment;
    }

    public void setGameFragment(GameFragment gameFragment) {
        this.mGameFragment = gameFragment;
    }

    public GameSelectionFragment getGameSelectionFragment() {
        return mGameSelectionFragment;
    }

    public void setGameSelectionFragment(GameSelectionFragment gameSelectionFragment) {
        this.mGameSelectionFragment = gameSelectionFragment;
    }

    public HistoryFragment getHistoryFragment() {
        return mHistoryFragment;
    }

    public void setHistoryFragment(HistoryFragment historyFragment) {
        this.mHistoryFragment = historyFragment;
    }

    public InstructionsFragment getInstructionsFragment() {
        return mInstructionsFragment;
    }

    public void setInstructionsFragment(InstructionsFragment instructionsFragment) {
        this.mInstructionsFragment = instructionsFragment;
    }

    public OnlineSelectionFragment getOnlineSelectionFragment() {
        return mOnlineSelectionFragment;
    }

    public void setOnlineSelectionFragment(OnlineSelectionFragment onlineSelectionFragment) {
        this.mOnlineSelectionFragment = onlineSelectionFragment;
    }

    private void popupRatingDialog() {
        // Popup asking to rate app after countdown
        int numTimesAppOpened = PreferenceManager.getDefaultSharedPreferences(this).getInt("num_times_app_opened_review", 0);
        if(numTimesAppOpened != -1) {
            numTimesAppOpened++;
            PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("num_times_app_opened_review", numTimesAppOpened).commit();
            if(numTimesAppOpened > 2) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch(which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt("num_times_app_opened_review", -1).commit();
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.sam.hex")));
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putInt("num_times_app_opened_review", -1).commit();
                            break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.review_popup_title).setMessage(R.string.review_popup_message)
                        .setPositiveButton(R.string.review_popup_ok, dialogClickListener).setNegativeButton(R.string.review_popup_never, dialogClickListener);

                // Wrap in try/catch because this can sometimes leak window
                try {
                    builder.show();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @param gameManager
     *            the gameManager to set
     */

    public boolean isIabSetup() {
        return mIabSetup;
    }

    public void setIabSetup(boolean iabSetup) {
        this.mIabSetup = iabSetup;
    }

    public void setOpenAchievements(boolean open) {
        this.mOpenAchievements = open;
    }

    public void setOpenOnlineSelectionFragment(boolean open) {
        this.mOpenOnlineSelectionFragment = open;
    }

    @Override
    public void switchToGame(Game game) {
        Bundle b = new Bundle();
        b.putBoolean(GameFragment.NET, true);

        mGameFragment = new GameFragment();
        mGameFragment.setGame(game);
        mGameFragment.setPlayer1Type(game.getPlayer1().getType());
        mGameFragment.setPlayer2Type(game.getPlayer2().getType());
        mGameFragment.setArguments(b);

        swapFragment(mGameFragment);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // No call for super(). Bug on API Level > 11.
    }
}
