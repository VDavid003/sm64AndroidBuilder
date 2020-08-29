package com.vdavid003.sm64androidbuilder;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;

public class FilePreference extends Preference {
    private String mPath;

    public FilePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FilePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Preference);
    }

    public FilePreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.preferenceStyle);
    }

    public FilePreference(Context context) {
        super(context, null);
    }

    public void setPath(String path) {
        mPath = path;
        persistString(path);
        notifyChanged();
    }

    final ActivityResultLauncher<String[]> mOD = ((AppCompatActivity)getContext()).registerForActivityResult(new ActivityResultContracts.OpenDocument(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        ((AppCompatActivity)getContext()).getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        String filePath = uri.toString();
                        setPath(filePath);
                    }
                }
            });


    @Override
    public CharSequence getSummary() {
        return super.getSummary().toString().replaceAll("%s", mPath);
    }

    @Override
    protected void onClick() {
        mOD.launch(new String[]{"*/*"});
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setPath(getPersistedString((String) defaultValue));
    }
}
