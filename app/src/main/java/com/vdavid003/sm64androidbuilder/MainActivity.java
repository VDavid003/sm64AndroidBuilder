package com.vdavid003.sm64androidbuilder;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    AlertDialog pathInvalidDialog;
    AlertDialog pathUnsetDialog;
    AlertDialog updateDialog;
    final FragmentManager fm = getSupportFragmentManager();
    final SetupDialog setupDialog = SetupDialog.newInstance();
    RadioGroup branch;
    LinearLayout flags;

    final Map<String, boolean[]> FlagsMap = new HashMap<String, boolean[]>() {
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

        updateDialog = new AlertDialog.Builder(this)
                .setTitle("Update available!")
                .setMessage("Download it now?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/VDavid003/sm64AndroidBuilder/releases")));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();

        UpdateCheckerThread.start();

        //https://stackoverflow.com/a/7636468
        DialogInterface.OnShowListener onShowListener = new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button1 = pathInvalidDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                Button button2 = pathUnsetDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
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

        flags = findViewById(R.id.flags);
        loadFlags(flags);

        branch = findViewById(R.id.branch);
        branch.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                RadioButton radioButton = radioGroup.findViewById(i);
                setEnableFlags(radioButton);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("branch", radioGroup.indexOfChild(radioButton));
                editor.apply();
            }
        });
        if(sharedPreferences.contains("branch")) {
            ((RadioButton)branch.getChildAt(sharedPreferences.getInt("branch", 1))).setChecked(true);
        }

        setEnableFlags((RadioButton)branch.findViewById(branch.getCheckedRadioButtonId()));

        ((TextView)findViewById(R.id.jobsInput)).setText(Integer.toString(sharedPreferences.getInt("jobs", 4)));
        ((CheckBox)findViewById(R.id._60fpsPatch)).setChecked(sharedPreferences.getBoolean("60fps", false));
        ((CheckBox)findViewById(R.id.r96Patch)).setChecked(sharedPreferences.getBoolean("render96", false));
        ((CheckBox)findViewById(R.id.dynosPatch)).setChecked(sharedPreferences.getBoolean("dynos", false));
    }

    private void setEnableFlags(RadioButton selected) {
        int i = 0;
        for (CheckBox checkBox : getFlagCheckboxes(flags)) {
            checkBox.setEnabled(FlagsMap.get(selected.getText())[i]);
            i++;
        }
        findViewById(R.id.r96Patch).setEnabled(selected.getText().equals("ex/nightly"));
        findViewById(R.id._60fpsPatch).setEnabled(!selected.getText().equals("ex/master"));
        findViewById(R.id.dynosPatch).setEnabled(selected.getText().equals("ex/nightly"));
    }

    @Override
    public void onPause() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("jobs", Integer.parseInt(((TextView)findViewById(R.id.jobsInput)).getText().toString()));
        editor.putBoolean("60fps", ((CheckBox)findViewById(R.id._60fpsPatch)).isChecked());
        editor.putBoolean("render96", ((CheckBox)findViewById(R.id.r96Patch)).isChecked());
        editor.putBoolean("dynos", ((CheckBox)findViewById(R.id.dynosPatch)).isChecked());
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
        List<CheckBox> checkBoxList = new ArrayList<>();
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

    final Thread UpdateCheckerThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try  {
                URLConnection connection = (new URL("https://raw.githubusercontent.com/VDavid003/sm64AndroidBuilder/master/appVersion")).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                // Read and store the result line by line then return the entire string.
                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                int onlineVersion = Integer.parseInt(reader.readLine());
                in.close();

                if (onlineVersion > BuildConfig.VERSION_CODE) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            updateDialog.show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

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

    //Basically DocumentsContract.copyDocument with support for API level below 24
    private void copyDocument_compatible(Uri sourceFile, Uri targetDir, String mimeType, String name) throws IOException {
        ParcelFileDescriptor sourceFileDescriptor = getContentResolver().openFileDescriptor(sourceFile, "r");
        FileInputStream fileInputStream = new FileInputStream(sourceFileDescriptor.getFileDescriptor());

        String targetDocId = DocumentsContract.getTreeDocumentId(targetDir);
        Uri targetDirUri = DocumentsContract.buildDocumentUriUsingTree(targetDir, targetDocId );
        Uri targetFile = DocumentsContract.createDocument(getContentResolver(), targetDirUri, mimeType, name);

        ParcelFileDescriptor targetFileDescriptor = getContentResolver().
                openFileDescriptor(targetFile, "w");
        FileOutputStream fileOutputStream =
                new FileOutputStream(targetFileDescriptor.getFileDescriptor());

        byte[] buf = new byte[1024];
        int len;
        while ((len = fileInputStream.read(buf)) > 0) {
            fileOutputStream.write(buf, 0, len);
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
                Uri targetUri = Uri.parse(sharedPreferences.getString("termuxDir", ""));
                copyDocument_compatible(sourceFile, targetUri, "application/octet-stream", getBaseRomFileName());

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getBaseRomFileName() +"SHA", sharedPreferences.getString("selectedSHA", ""));
                editor.apply();
            } catch (Exception e) {
                Toast.makeText(this, "Error while copying baserom!", Toast.LENGTH_SHORT).show();
                return;
            }

            String buildString = "./sm64AndroidBuilder2 make -b " + GetBranch() + GetOptions() + "-p \"" + GetFlags(flags) + "-j" + sharedPreferences.getInt("jobs", 4) + '"' + GetPatches();

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

    private String GetPatches() {
        CheckBox r96 = findViewById(R.id.r96Patch);
        CheckBox _60fps = findViewById(R.id._60fpsPatch);
        CheckBox dynos = findViewById(R.id.dynosPatch);
        if (!(r96.isEnabled() && r96.isChecked()) &&
            !(_60fps.isEnabled() && _60fps.isChecked()) &&
            !(dynos.isEnabled() && dynos.isChecked()))
            return "";
        StringBuilder str = new StringBuilder(" -pa \"");
        if (r96.isEnabled() && r96.isChecked()) {
            str.append("render96_android ");
        }
        if (dynos.isEnabled() && dynos.isChecked()) {
            str.append("DynOS.1.0 ");
        }
        if (_60fps.isEnabled() && _60fps.isChecked()) {
            str.append("60fps");
            if (((RadioButton)branch.findViewById(branch.getCheckedRadioButtonId())).getText().equals("ex/nightly")) {
                str.append("_ex");
            }
        }
        str.append('"');
        return str.toString();
    }

    public void download(View view) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org/en/packages/com.termux/")));
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

    public void installData(View view) {
        if (sharedPreferences.getString("sm64Dir", "") == "") {
            Toast.makeText(this, "Please set your com.vdavid003.sm64port/files directory", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        } else if (!DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("sm64Dir", ""))).exists()) {
            Toast.makeText(this, "Your com.vdavid003.sm64port/files directory is invalid", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }

        Uri sourceFile;
        try {
            sourceFile = DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("termuxDir", "")))
                    .findFile("build")
                    .findFile(getVersionString().toLowerCase() + "_pc")
                    .findFile("res")
                    .findFile("base.zip").getUri();
        } catch (Exception e) {
            Toast.makeText(this, "Base.zip not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri targetUri = Uri.parse(sharedPreferences.getString("sm64Dir", ""));
        if (DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("sm64Dir", ""))).findFile("base.zip") != null) {
            DocumentFile.fromTreeUri(this, Uri.parse(sharedPreferences.getString("sm64Dir", ""))).findFile("base.zip").delete();
        }
        try {
            copyDocument_compatible(sourceFile, targetUri, "application/zip", "base.zip");
        } catch (Exception e) {
            Toast.makeText(this, "Error trying to copy base.zip!", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, "Base.zip successfully installed!", Toast.LENGTH_SHORT).show();
    }

    final ActivityResultLauncher<String[]> zipSelector = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri uri) {
                    if (uri != null) {
                        Uri targetUri = Uri.parse(sharedPreferences.getString("termuxDir", ""));

                        String fileName;
                        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                            cursor.moveToFirst();
                            fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            if (DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(sharedPreferences.getString("termuxDir", ""))).findFile(fileName) != null) {
                                DocumentFile.fromTreeUri(getApplicationContext(), Uri.parse(sharedPreferences.getString("termuxDir", ""))).findFile(fileName).delete();
                            }
                            copyDocument_compatible(uri, targetUri, "application/zip", fileName);
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "Error trying to copy zip!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("code", "pushd sm64-port-android && unzip -o " + fileName + " && rm " + fileName + " && popd");
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(getApplicationContext(), "Paste command inside Termux!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    public void copyZip(View view) {
        zipSelector.launch(new String[]{"application/zip"});
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
