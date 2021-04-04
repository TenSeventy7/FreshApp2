package de.dlyt.yanndroid.freshapp.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SeslProgressBar;

import com.google.android.material.appbar.AppBarLayout;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import de.dlyt.yanndroid.freshapp.R;
import de.dlyt.yanndroid.freshapp.download.DownloadAddon;
import de.dlyt.yanndroid.freshapp.tasks.AddonXmlParser;
import de.dlyt.yanndroid.freshapp.utils.Addon;
import de.dlyt.yanndroid.freshapp.utils.Constants;
import de.dlyt.yanndroid.freshapp.utils.Preferences;
import de.dlyt.yanndroid.freshapp.utils.RomUpdate;
import de.dlyt.yanndroid.freshapp.utils.Utils;
import in.uncod.android.bypass.Bypass;

public class AddonActivity extends AppCompatActivity implements Constants {

    public final static String TAG = "AddonActivity";

    public static Context mContext;
    private static ListView mListview;
    private static DownloadAddon mDownloadAddon;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ota_addons);

        initToolbar();
        settilte("Addons");

        mListview = (ListView) findViewById(R.id.listview);
        mDownloadAddon = new DownloadAddon();

        String isRomhut = "";

        if (!RomUpdate.getRomHut(mContext).equals("null")) {
            isRomhut = "?order_by=name&order_direction=asc";
        }

        new LoadAddonManifest(mContext).execute(RomUpdate.getAddonsUrl(mContext) + isRomhut);
    }


    public void initToolbar() {
        /** Def */
        androidx.appcompat.widget.Toolbar toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        AppBarLayout AppBar = findViewById(R.id.app_bar);

        TextView expanded_title = findViewById(R.id.expanded_title);
        TextView expanded_subtitle = findViewById(R.id.expanded_subtitle);
        TextView collapsed_title = findViewById(R.id.collapsed_title);

        /** 1/3 of the Screen */
        ViewGroup.LayoutParams layoutParams = AppBar.getLayoutParams();
        layoutParams.height = (int) ((double) this.getResources().getDisplayMetrics().heightPixels / 2.6);


        /** Collapsing */
        AppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                float percentage = (AppBar.getY() / AppBar.getTotalScrollRange());
                expanded_title.setAlpha(1 - (percentage * 2 * -1));
                expanded_subtitle.setAlpha(1 - (percentage * 2 * -1));
                collapsed_title.setAlpha(percentage * -1);
            }
        });

        /**Back*/
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


    }

    public void settilte(String title) {
        TextView expanded_title = findViewById(R.id.expanded_title);
        TextView collapsed_title = findViewById(R.id.collapsed_title);
        expanded_title.setText(title);
        collapsed_title.setText(title);
    }


    public void setupListView(ArrayList<Addon> addonsList) {
        final AddonsArrayAdapter adapter = new AddonsArrayAdapter(mContext, addonsList);
        if (mListview != null) {
            mListview.setAdapter(adapter);
        }
    }

    public static class AddonsArrayAdapter extends ArrayAdapter<Addon> {

        AddonsArrayAdapter(Context context, ArrayList<Addon> users) {
            super(context, 0, users);
        }

        public static void updateProgress(int index, int progress, boolean finished) {
            View v = mListview.getChildAt((index - 1) -
                    mListview.getFirstVisiblePosition());

            if (v == null) {
                return;
            }

            SeslProgressBar progressBar = (SeslProgressBar) v.findViewById(R.id.progress_bar);

            if (finished) {
                progressBar.setProgress(0);
            } else {
                progressBar.setProgress(progress);
            }
        }

        public static void updateButtons(int index, boolean finished) {
            View v = mListview.getChildAt((index - 1) -
                    mListview.getFirstVisiblePosition());

            if (v == null) {
                return;
            }

            final Button download = (Button) v.findViewById(R.id.download_button);
            final Button cancel = (Button) v.findViewById(R.id.cancel_button);
            final Button delete = (Button) v.findViewById(R.id.delete_button);

            if (finished) {
                download.setVisibility(View.VISIBLE);
                download.setText(mContext.getResources().getString(R.string.finished));
                download.setClickable(false);
                delete.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
            } else {
                download.setVisibility(View.VISIBLE);
                download.setText(mContext.getResources().getString(R.string.download));
                download.setClickable(true);
                cancel.setVisibility(View.GONE);
                delete.setVisibility(View.GONE);
            }
        }

        private void showNetworkDialog() {
            Builder mNetworkDialog = new Builder(mContext, R.style.AlertDialogStyle);
            mNetworkDialog.setTitle(R.string.available_wrong_network_title)
                    .setMessage(R.string.available_wrong_network_message)
                    .setPositiveButton(R.string.ok, null)
                    .setNeutralButton(R.string.settings, (dialog, which) -> {
                        Intent intent = new Intent(mContext, SettingsActivity.class);
                        mContext.startActivity(intent);
                    });

            mNetworkDialog.show();
        }

        private void deleteConfirm(final File file, final Addon item) {
            Builder deleteConfirm = new Builder(mContext, R.style.AlertDialogStyle);
            deleteConfirm.setTitle(R.string.delete);
            deleteConfirm.setMessage(mContext.getResources().getString(R.string.delete_confirm) +
                    "\n\n" + file.getName());
            deleteConfirm.setPositiveButton(R.string.ok, (dialog, which) -> {
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (!deleted) Log.e(TAG, "Unable to delete file...");
                    updateButtons(item.getId(), false);
                }
            });
            deleteConfirm.setNegativeButton(R.string.cancel, null);
            deleteConfirm.show();
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final Addon item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.card_addons_list_item, parent, false);
            }

            TextView title = (TextView) convertView.findViewById(R.id.title);
            TextView desc = (TextView) convertView.findViewById(R.id.description);
            TextView updatedOn = (TextView) convertView.findViewById(R.id.updatedOn);
            TextView filesize = (TextView) convertView.findViewById(R.id.size);
            final Button download = (Button) convertView.findViewById(R.id.download_button);
            final Button cancel = (Button) convertView.findViewById(R.id.cancel_button);
            final Button delete = (Button) convertView.findViewById(R.id.delete_button);

            assert item != null;
            title.setText(item.getTitle());

            Bypass byPass = new Bypass(mContext);
            String descriptionStr = item.getDesc();
            CharSequence string = byPass.markdownToSpannable(descriptionStr);
            desc.setText(string);
            desc.setMovementMethod(LinkMovementMethod.getInstance());

            String UpdatedOnStr = convertView.getResources().getString(R.string.addons_updated_on);
            String date = item.getUpdatedOn();

            Locale locale = Locale.getDefault();
            DateFormat fromDate = new SimpleDateFormat("yyyy-MM-dd", locale);
            DateFormat toDate = new SimpleDateFormat("dd, MMMM yyyy", locale);

            try {
                date = toDate.format(fromDate.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            updatedOn.setText(UpdatedOnStr + " " + date);

            filesize.setText(Utils.formatDataFromBytes(item.getFilesize()));
            final File file = new File(SD_CARD
                    + File.separator
                    + OTA_DOWNLOAD_DIR, item.getTitle() + ".zip");

            if (DEBUGGING) {
                Log.d(TAG, "file path " + file.getAbsolutePath());
                Log.d(TAG, "file length " + file.length() + " remoteLength " + item.getFilesize());
            }
            boolean finished = file.length() == item.getFilesize();
            if (finished) {
                download.setVisibility(View.VISIBLE);
                download.setText(mContext.getResources().getString(R.string.finished));
                download.setClickable(false);
                delete.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
            } else {
                download.setVisibility(View.VISIBLE);
                download.setText(mContext.getResources().getString(R.string.download));
                download.setClickable(true);
                cancel.setVisibility(View.GONE);
                delete.setVisibility(View.GONE);
            }

            download.setOnClickListener(v -> {
                boolean isMobile = Utils.isMobileNetwork(mContext);
                boolean isSettingWiFiOnly = Preferences.getNetworkType(mContext).equals
                        (WIFI_ONLY);

                if (isMobile && isSettingWiFiOnly) {
                    showNetworkDialog();
                } else {
                    mDownloadAddon.startDownload(mContext, item.getDownloadLink(), item
                            .getTitle(), item.getId());
                    download.setVisibility(View.GONE);
                    cancel.setVisibility(View.VISIBLE);
                }
            });

            cancel.setOnClickListener(v -> {
                mDownloadAddon.cancelDownload(mContext, item.getId());
                download.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
                updateProgress(item.getId(), 0, true);
            });
            delete.setOnClickListener(v -> deleteConfirm(file, item));
            return convertView;
        }
    }

    private class LoadAddonManifest extends AsyncTask<Object, Void, ArrayList<Addon>> {

        private static final String MANIFEST = "addon_manifest.xml";
        public final String TAG = this.getClass().getSimpleName();
        private ProgressDialog mLoadingDialog;

        private Context mContext;

        LoadAddonManifest(Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {

            // Show a loading/progress dialog while the search is being performed
            mLoadingDialog = new ProgressDialog(mContext, R.style.AlertDialogStyle);
            mLoadingDialog.setIndeterminate(true);
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.setMessage(mContext.getResources().getString(R.string.loading));
            mLoadingDialog.show();

            // Delete any existing manifest file before we attempt to download a new one
            File manifest = new File(mContext.getFilesDir().getPath(), MANIFEST);
            if (manifest.exists()) {
                boolean deleted = manifest.delete();
                if (!deleted) Log.e(TAG, "Unable to delete manifest file...");
            }
        }

        @Override
        protected ArrayList<Addon> doInBackground(Object... param) {

            try {
                InputStream input;

                URL url = new URL((String) param[0]);
                URLConnection connection = url.openConnection();
                connection.connect();
                // download the file
                input = new BufferedInputStream(url.openStream());

                OutputStream output = mContext.openFileOutput(
                        MANIFEST, Context.MODE_PRIVATE);

                byte data[] = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                // file finished downloading, parse it!
                AddonXmlParser parser = new AddonXmlParser();
                return parser.parse(new File(mContext.getFilesDir(), MANIFEST));
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Addon> result) {
            mLoadingDialog.cancel();
            if (result != null) {
                setupListView(result);
            }
            super.onPostExecute(result);
        }
    }
}