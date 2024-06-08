package com.google.samples.smartlock.sms_verify.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialPickerConfig;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.samples.smartlock.sms_verify.PrefsHelper;
import com.google.samples.smartlock.sms_verify.R;

abstract class PhoneNumberActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = PhoneNumberActivity.class.getSimpleName();


    private static final int RC_HINT = 1000;

    private PrefsHelper prefs;
    protected PhoneNumberUi ui;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_number);
        ui = new PhoneNumberUi(findViewById(R.id.phone_number), getActivityTitle());

        String defaultPhone = getPrefs().getPhoneNumber(null);
        if (defaultPhone != null) {
            ui.setPhoneNumber(defaultPhone);
        }

        initActivityResult();
    }

    protected abstract String getActivityTitle();

    protected abstract void doSubmit(String phoneValue);

    public void initActivityResult() {
        intentSenderLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Credential cred = result.getData().getParcelableExtra(Credential.EXTRA_KEY);
                Log.e(TAG, "onActivityResult: data=" + cred.getId());
                ui.setPhoneNumber(cred.getId());
            } else {
                ui.focusPhoneNumber();
            }
        });
    }

    private ActivityResultLauncher<IntentSenderRequest> intentSenderLauncher;

    private void showHint() {
        ui.clearKeyboard();
        HintRequest hintRequest = new HintRequest.Builder()
                .setHintPickerConfig(new CredentialPickerConfig.Builder()
                        .setShowCancelButton(true)
                        .build())
                .setPhoneNumberIdentifierSupported(true)
//                .setEmailAddressIdentifierSupported(true)
                .build();
        CredentialsClient credentialsClient = Credentials.getClient(this);
        PendingIntent intent = credentialsClient.getHintPickerIntent(hintRequest);
        try {
            IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(intent.getIntentSender()).build();
            intentSenderLauncher.launch(intentSenderRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected PrefsHelper getPrefs() {
        if (prefs == null) {
            prefs = new PrefsHelper(this);
        }
        return prefs;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "GoogleApiClient 因原因代码而被暂停: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient 连接失败: " + connectionResult);
    }

    class PhoneNumberUi implements View.OnClickListener {
        private final FocusControl phoneFocus;
        private TextView title;
        private EditText phoneField;
        private Button submit;

        PhoneNumberUi(View root, String activityTitle) {
            title = (TextView) root.findViewById(R.id.phone_number_title);
            phoneField = (EditText) root.findViewById(R.id.phone_number_field);
            submit = (Button) root.findViewById(R.id.phone_number_submit);
            phoneFocus = new FocusControl(phoneField);

            title.setText(activityTitle);
            submit.setOnClickListener(this);
            phoneField.setOnClickListener(this);
            setSubmitEnabled(true);
        }

        @Override
        public void onClick(View view) {
            if (view.equals(submit)) {
                doSubmit(getPhoneNumber());
            }
            if (view.equals(phoneField)) {
                phoneField.setEnabled(true);
                phoneField.requestFocus();
                if (TextUtils.isEmpty(getPhoneNumber())) {
                    showHint();
                }
            }
        }

        void setPhoneNumber(String phoneNumber) {
            phoneField.setText(phoneNumber);
        }

        String getPhoneNumber() {
            return phoneField.getText().toString();
        }

        void focusPhoneNumber() {
            phoneFocus.showKeyboard();
        }

        void clearKeyboard() {
            if(phoneField!=null) phoneFocus.hideKeyboard();
        }

        void setSubmitEnabled(boolean enabled) {
            submit.setEnabled(enabled);
        }
    }

    class FocusControl {
        static final int POST_DELAY = 250;
        private Handler handler;
        private InputMethodManager manager;
        private View focus;

        /**
         * Keyboard focus controller
         *
         * Shows and hides the keyboard. Uses a runnable to do the showing as there are race
         * conditions with hiding the keyboard that this solves.
         *
         * @param focus The view element to focus and hide the keyboard from
         */
        public FocusControl(View focus) {
            handler = new Handler();
            manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            this.focus = focus;
        }

        /**
         * Focus the view and show the keyboard.
         */
        public void showKeyboard() {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    focus.requestFocus();
                    manager.showSoftInput(focus, InputMethodManager.SHOW_IMPLICIT);
                }
            }, POST_DELAY);
        }

        /**
         * Hide the keyboard.
         */
        public void hideKeyboard() {
            View currentView = getCurrentFocus();
            if (currentView.equals(focus)) {
                manager.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
            }
        }
    }
}
