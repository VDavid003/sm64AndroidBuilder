package com.vdavid003.sm64androidbuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    AlertDialog pathInvalidDialog;
    AlertDialog pathUnsetDialog;
    final FragmentManager fm = getSupportFragmentManager();
    final SetupDialog setupDialog = SetupDialog.newInstance();
    RadioGroup branch;
    LinearLayout flags;

    Map<String, boolean[]> FlagsMap = new HashMap<String, boolean[]>() {
        {
            put("master", new boolean[]{
                    true, //TOUCH_CONTROLS
                    false, //NODRAWINGDISTANCE
                    false, //EXT_OPTIONS_MENU
                    false, //EXTERNAL_DATA
                    false, //BETTERCAMERA
                    false, //TEXTURE_FIX
                    false, //TEXTSAVES
            });
            put("ex/master", new boolean[]{
                    true, //TOUCH_CONTROLS
                    true, //NODRAWINGDISTANCE
                    true, //EXT_OPTIONS_MENU
                    true, //EXTERNAL_DATA
                    true, //BETTERCAMERA
                    true, //TEXTURE_FIX
                    true //TEXTSAVES
            });
            put("ex/nightly", new boolean[]{
                    true, //TOUCH_CONTROLS
                    true, //NODRAWINGDISTANCE
                    true, //EXT_OPTIONS_MENU
                    true, //EXTERNAL_DATA
                    true, //BETTERCAMERA
                    true, //TEXTURE_FIX
                    true //TEXTSAVES
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //showTermuxMessage();

        AlertDialog.Builder pathErrorDialog =
            new AlertDialog.Builder(this)
                //Message set before showing
                .setCancelable(false)
                .setPositiveButton("Set path", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                    }
                })
                .setNeutralButton("Show initial setup", null)
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.finishAffinity();
                    }
                });
        pathInvalidDialog = pathErrorDialog
                .setTitle("One or more paths are invalid!")
                .setMessage("Please set your paths again!\n" +
                            "(Maybe something was deleted?)").create();
        pathUnsetDialog = pathErrorDialog
                .setTitle("One or more paths are not set!")
                .setMessage("Setting the in-termux sm64-port-android directory, and your baserom is necessary!").create();

        //https://stackoverflow.com/a/7636468
        DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button1 = ((AlertDialog) pathInvalidDialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                Button button2 = ((AlertDialog) pathUnsetDialog).getButton(AlertDialog.BUTTON_NEUTRAL);
                View.OnClickListener onClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setupDialog.show(fm, "setup_dialog");
                    }
                };
                if (button1 != null)
                    button1.setOnClickListener(onClickListener);
                if (button2 != null)
                    button2.setOnClickListener(onClickListener);
            }
        };
        pathInvalidDialog.setOnShowListener(onShowListener);
        pathUnsetDialog.setOnShowListener(onShowListener);

        branch = findViewById(R.id.branch);
        if(sharedPreferences.contains("branch")) {
            ((RadioButton)branch.getChildAt(sharedPreferences.getInt("branch", 1))).setChecked(true);
        }

        flags = findViewById(R.id.flags);
        loadFlags(flags);

        branch.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton radioButton = radioGroup.findViewById(i);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("branch", radioGroup.indexOfChild(radioButton));
                editor.apply();
                int j = 0;
                for (CheckBox checkBox : getFlagCheckboxes(flags)) {
                    checkBox.setEnabled(FlagsMap.get(radioButton.getText())[j]);
                    j++;
                }
            }
        });

        RadioButton selectedBranch = branch.findViewById(branch.getCheckedRadioButtonId());
        if (selectedBranch != null) {
            int j = 0;
            for (CheckBox checkBox : getFlagCheckboxes(flags)) {
                checkBox.setEnabled(FlagsMap.get(selectedBranch.getText())[j]);
                j++;
            }
        }

        ((TextView)findViewById(R.id.jobsInput)).setText(Integer.toString(sharedPreferences.getInt("jobs", 4)));
    }

    @Override
    public void onPause() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("jobs", Integer.parseInt(((TextView)findViewById(R.id.jobsInput)).getText().toString()));
        saveFlags(flags, editor);
        editor.apply();
        super.onPause();
    }

    private void saveFlags(LinearLayout parent, SharedPreferences.Editor editor) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view instanceof LinearLayout)
                saveFlags((LinearLayout) view, editor);
            else if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                editor.putBoolean((String)checkBox.getText(), checkBox.isChecked());
            }
        }
    }

    private void loadFlags(LinearLayout parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view instanceof LinearLayout)
                loadFlags((LinearLayout) view);
            else if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                if (sharedPreferences.contains((String)checkBox.getText()))
                    checkBox.setChecked(sharedPreferences.getBoolean((String)checkBox.getText(), false));
            }
        }
    }

    private List<CheckBox> getFlagCheckboxes(LinearLayout parent) {
        List<CheckBox> checkBoxList = new ArrayList<CheckBox>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view instanceof LinearLayout)
                checkBoxList.addAll(getFlagCheckboxes((LinearLayout) view));
            else if (view instanceof CheckBox) {
                checkBoxList.add((CheckBox) view);
            }
        }
        return checkBoxList;
    }

    private boolean testPaths() {
        if ((sharedPreferences.getString("termuxDir", "") == "") || (sharedPreferences.getString("baseROM", "") == "")) {
            pathUnsetDialog.show();
            return false;
        } else if ((!DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("termuxDir", ""))).exists()) ||
                (!DocumentFile.fromSingleUri(this, Uri.parse(sharedPreferences.getString("baseROM", ""))).exists())) {
            pathInvalidDialog.show();
            return false;
        }
        return true;
    }

    private String getVersionString() {
        String version = sharedPreferences.getString("version", "");
        if (version.equals("auto"))
            version = sharedPreferences.getString("autoVersion", "");
        if (version.equals(""))
            version = "US";
        return version;
    }

    private String getBaseRomFileName() {
        return "baserom." + getVersionString().toLowerCase() + ".z64";
    }

    @Override
    protected void onStart() {
        super.onStart();
        testPaths();
        //After this just so it appears above the path message.
        if(!sharedPreferences.getBoolean("initDone", false)) {
            setupDialog.show(fm, "setup_dialog");
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("initDone", true);
            editor.apply();
        }
    }

    //Options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settingsButton:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.initButton:
                setupDialog.show(fm, "setup_dialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void copyBuild(View view) {
        if (testPaths()) {
            //Delete it if it's different and it exists inside Termux
            if (!(sharedPreferences.getString("selectedSHA", "").equals(sharedPreferences.getString(getBaseRomFileName() +"SHA", ""))) &&
                 (DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("termuxDir", ""))).findFile(getBaseRomFileName()) != null)) {
                DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("termuxDir", ""))).findFile(getBaseRomFileName()).delete();
            }

            if (DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("termuxDir", ""))).findFile(getBaseRomFileName()) == null)
            try {
                Uri sourceFile = Uri.parse(sharedPreferences.getString("baseROM", ""));
                ParcelFileDescriptor sourceFileDescriptor = getContentResolver().openFileDescriptor(sourceFile, "r");
                FileInputStream fileInputStream = new FileInputStream(sourceFileDescriptor.getFileDescriptor());

                Uri targetUri = Uri.parse(sharedPreferences.getString("termuxDir", ""));
                String targetDocId = DocumentsContract.getTreeDocumentId(targetUri);
                Uri targetDirUri = DocumentsContract.buildDocumentUriUsingTree(targetUri, targetDocId );
                Uri targetFile = DocumentsContract.createDocument(getContentResolver(), targetDirUri, "application/octet-stream", getBaseRomFileName());
                ParcelFileDescriptor targetFileDescriptor = getContentResolver().
                        openFileDescriptor(targetFile, "w");
                FileOutputStream fileOutputStream =
                        new FileOutputStream(targetFileDescriptor.getFileDescriptor());

                byte[] buf = new byte[1024];
                int len;
                while ((len = fileInputStream.read(buf)) > 0) {
                    fileOutputStream.write(buf, 0, len);
                }

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getBaseRomFileName() +"SHA", sharedPreferences.getString("selectedSHA", ""));
                editor.apply();
            } catch (Exception e) {
                Toast.makeText(this, "Error while copying baserom!", Toast.LENGTH_SHORT).show();
                return;
            }

            String buildString = "./sm64AndroidBuilder make -b " + GetBranch() + GetOptions() + "-p \"" + GetFlags(flags) + "-j" + sharedPreferences.getInt("jobs", 4) + '"';

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("code", buildString);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
        }
    }

    private String GetBranch() {
        int buttonId = branch.getCheckedRadioButtonId();
        RadioButton radioButton = branch.findViewById(buttonId);
        if(radioButton == null) return "master";
        return radioButton.getText().toString();
    }

    private String GetOptions() {
        String str = " ";
        CheckBox update = findViewById(R.id.update);
        CheckBox clean = findViewById(R.id.clean);
        CheckBox reset = findViewById(R.id.reset);
        if(update.isChecked())
            str += "-u ";
        if(clean.isChecked())
            str += "-c ";
        if(reset.isChecked())
            str += "-r ";
        return str;
    }

    private String GetFlags(LinearLayout parent) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View view = parent.getChildAt(i);
            if (view instanceof LinearLayout)
                str.append(GetFlags((LinearLayout) view));
            else if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                if(checkBox.isEnabled())
                    str.append(checkBox.getText()).append("=").append(checkBox.isChecked() ? "1" : "0").append(" ");
            }
        }
        return str.toString();
    }

    public void download(View view) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.termux")));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.termux")));
            }
        }
        else {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/download/termux-repositories-legacy/termux-app-git-debug.apk")));
        }
    }

    public void copySetup(View view) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("code", getString(R.string.setup_command));
        clipboard.setPrimaryClip(clip);
    }

    public void installApk(View view) {
        //TODO this is Android 7+ only. Why can't they just keep old methods working? This is retarded.
        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        Uri apkFile;
        try {
            //Is there some better way to do this?
            apkFile = DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("termuxDir", "")))
                    .findFile("build")
                    .findFile(getVersionString().toLowerCase() + "_pc")
                    .findFile("sm64." + getVersionString().toLowerCase() + ".f3dex2e.apk").getUri();
        } catch (Exception e) {
            Toast.makeText(this, "APK not found!", Toast.LENGTH_SHORT).show();
            return;
        }
        install.setDataAndType(apkFile,"application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(install);
    }

    //https://stackoverflow.com/questions/18752202/check-if-application-is-installed-android
    //Doesn't work
    /*private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void showTermuxMessage() {
        if(!isPackageInstalled("com.termux", this.getPackageManager())); {
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Termux is not installed!")
                            .setMessage("Termux is required for this app.\nDo you want to install it?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.termux")));
                                    } catch (android.content.ActivityNotFoundException anfe) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.termux")));
                                    }
                                }
                            })
                            .setNeutralButton("Ignore", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.finishAffinity();
                                }

                            });
            alertDialogBuilder.show();
        }
    }*/
}
