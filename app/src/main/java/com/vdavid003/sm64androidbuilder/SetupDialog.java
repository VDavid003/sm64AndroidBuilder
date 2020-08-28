package com.vdavid003.sm64androidbuilder;

import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.DialogFragment;

public class SetupDialog extends DialogFragment {

    public SetupDialog() {
    }

    public static SetupDialog newInstance() {
        return new SetupDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setup_dialog, container);
        // Doesn't work. Why?
        // getDialog().setTitle("Initial Setup");

        TextView downloadText = view.findViewById(R.id.setupTermuxDownloadText);
        String strText;
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            strText = "Click below to download it from the Play Store!";
        } else
        {
            strText = "Click below to download the Android 5.x-6.x compatible version!";
        }
        downloadText.setText(strText);

        EditText copyText = view.findViewById(R.id.copySetupText);
        copyText.setInputType(InputType.TYPE_NULL);
        copyText.setTextIsSelectable(true);
        copyText.setKeyListener(null);

        return view;
    }

}
