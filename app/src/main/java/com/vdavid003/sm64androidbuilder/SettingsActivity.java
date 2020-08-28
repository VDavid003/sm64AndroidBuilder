package com.vdavid003.sm64androidbuilder;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.FileInputStream;
import java.security.MessageDigest;

public class SettingsActivity extends AppCompatActivity {

    private static SharedPreferences sharedPreferences;
    private AlertDialog.Builder noMatch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //Auto detect ROM version
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        noMatch = new AlertDialog.Builder(this)
                      .setTitle("The selected base ROM doesn't seem to match any version!")
                      .setMessage("This means that your ROM file is probably not going to work.\n" +
                              "You can ignore this message but keep in mind that problems will probably occur.")
                      .setPositiveButton("Set path", new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int which) {
                              SettingsFragment.baserom.onClick();
                          }
                      })
                      .setNeutralButton("Ignore", new DialogInterface.OnClickListener() {
                          public void onClick(DialogInterface dialog, int which) {
                          }

                      });
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        static ListPreference versions;
        static FilePreference baserom;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            versions = findPreference("version");
            baserom = findPreference("baseROM");
            UpdateAutoVersion(sharedPreferences.getString("autoVersion", ""));
        }

        static void UpdateAutoVersion(String version) {
            String autoString;
            switch (version) {
                case "US":
                    autoString = "Automatic (US)";
                    break;
                case "EU":
                    autoString = "Automatic (EU)";
                    break;
                case "JP":
                    autoString = "Automatic (JP)";
                    break;
                case "SH":
                    autoString = "Automatic (SH)";
                    break;
                default:
                    autoString = "Automatic (Unknown, assuming US!)";
                    break;
            }
            versions.setEntries(new CharSequence[] {
                    autoString,
                    "US", "EU", "JP", "SH"
            });
        }
    }

    final SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.equals("baseROM")) try {
                //Calculate SHA1
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                Uri sourceFile = Uri.parse(sharedPreferences.getString("baseROM", ""));
                ParcelFileDescriptor sourceFileDescriptor = getContentResolver().openFileDescriptor(sourceFile, "r");
                FileInputStream fileInputStream = new FileInputStream(sourceFileDescriptor.getFileDescriptor());

                int n = 0;
                byte[] buffer = new byte[8192];
                while (n != -1) {
                    n = fileInputStream.read(buffer);
                    if (n > 0)
                        digest.update(buffer, 0, n);
                }

                //Convert to hex
                byte[] messageDigest = digest.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : messageDigest)
                    hexString.append(Integer.toHexString(0xFF & b));

                //Compare with normal values
                String version = "";
                switch (hexString.toString()) {
                    case "9bef1128717f958171a4afac3ed78ee2bb4e86ce":
                        version = "US";
                        break;
                    case "4ac5721683d0e0b6bbb561b58a71740845dceea9":
                        version = "EU";
                        break;
                    case "8a20a5c83d6ceb0f0506cfc9fa20d8f438cafe51":
                        version = "JP";
                        break;
                    case "3f319ae697533a255a1003d09202379d78d5a2e0":
                        version = "SH";
                        break;
                }

                SettingsFragment.UpdateAutoVersion(version);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("autoVersion", version);
                editor.putString("selectedSHA", hexString.toString());
                editor.apply();
                if (version == "")
                    noMatch.show();
            } catch (Exception e) {
                //No idea why would this happen so let's just tell the user it's not recognised just so something happens.
                SettingsFragment.UpdateAutoVersion("");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("autoVersion", "");
                editor.putString("selectedSHA", "");
                editor.apply();
                noMatch.show();
            }
        }
    };
}