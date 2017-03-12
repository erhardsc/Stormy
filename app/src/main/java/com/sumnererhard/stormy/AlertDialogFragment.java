package com.sumnererhard.stormy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;

/**
 * Created by sumnererhard on 3/12/17.
 */

public class AlertDialogFragment extends DialogFragment {
    private String mErrorText;
    private String mErrorTitle;

    public String getErrorTitle() {
        return mErrorTitle;
    }

    public void setErrorTitle(String errorTitle) {
        mErrorTitle = errorTitle;
    }

    public String getErrorText() {
        return mErrorText;
    }

    public void setErrorText(String errorText) {
        mErrorText = errorText;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(getErrorTitle())
                .setMessage(getErrorText())
                .setPositiveButton(R.string.error_ok_button_text, null);

        AlertDialog dialog = builder.create();
        return dialog;
    }
}
