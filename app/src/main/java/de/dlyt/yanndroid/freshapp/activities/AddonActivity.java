package de.dlyt.yanndroid.freshapp.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog.Builder;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.AppBarLayout;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dlyt.yanndroid.freshapp.R;
import de.dlyt.yanndroid.freshapp.download.DownloadAddon;
import de.dlyt.yanndroid.freshapp.download.DownloadAddonProgress;
import de.dlyt.yanndroid.freshapp.tasks.AddonXmlParser;
import de.dlyt.yanndroid.freshapp.utils.Addon;
import de.dlyt.yanndroid.freshapp.utils.Constants;
import de.dlyt.yanndroid.freshapp.utils.AddonDownloadDB;
import de.dlyt.yanndroid.freshapp.utils.Preferences;
import de.dlyt.yanndroid.freshapp.utils.RomUpdate;
import de.dlyt.yanndroid.freshapp.utils.Utils;
import in.uncod.android.bypass.Bypass;

public class AddonActivity extends AppCompatActivity implements Constants {

    public final static String TAG = "AddonActivity";

    public static Context mContext;
    public static ImageLoader mImageLoader;
    private static ListView mListview;
    private static DownloadAddon mDownloadAddon;
    public static Handler UIHandler;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mContext = this;
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(mContext).build();
        mImageLoader = ImageLoader.getInstance();
        mImageLoader.init(config);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addons);

        initToolbar();
        settilte(getString(R.string.main_addon));
        setSubtitle(" ");

        mListview = (ListView) findViewById(R.id.listview);
        mDownloadAddon = new DownloadAddon();

        String isRomhut = "";

        if (!RomUpdate.getRomHut(mContext).equals("null")) {
            isRomhut = "?order_by=name&order_direction=asc";
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        UIHandler = new Handler(Looper.getMainLooper());
        mImageLoader = ImageLoader.getInstance();
        new LoadAddonManifest(mContext);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public static void runOnUI(Runnable runnable) {
        UIHandler.post(runnable);
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

        AppBar.setExpanded(false);

        /**Back*/
        ImageView navigationIcon = findViewById(R.id.navigationIcon);
        View navigationIcon_Badge = findViewById(R.id.navigationIcon_new_badge);
        navigationIcon_Badge.setVisibility(View.GONE);
        navigationIcon.setImageResource(R.drawable.ic_back);
        navigationIcon.setOnClickListener(new View.OnClickListener() {
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

    public void setSubtitle(String subtitle) {
        TextView expanded_subtitle = findViewById(R.id.expanded_subtitle);
        expanded_subtitle.setText(subtitle);
    }


    public void setupListView(ArrayList<Addon> addonsList) {
        final AddonsArrayAdapter adapter = new AddonsArrayAdapter(mContext, addonsList);
        mListview.removeAllViewsInLayout();
        mListview.setAdapter(adapter);
    }

    public static class AddonsArrayAdapter extends ArrayAdapter<Addon> {

        AddonsArrayAdapter(Context context, ArrayList<Addon> users) {
            super(context, 0, users);
        }

        public static void updateProgress(int index, int progress, boolean finished, int downloaded, boolean successful) {
            View view = mListview.getChildAt((index - 1) -
                    mListview.getFirstVisiblePosition());

            Long currentTime = System.currentTimeMillis();

            if (view == null) {
                return;
            }

            ProgressBar progressBar = view.findViewById(R.id.progress_bar);
            TextView downloadSpeed = (TextView) view.findViewById(R.id.download_speed);
            TextView startTimeText = (TextView) view.findViewById(R.id.start_time);
            Long startTime = Long.parseLong(String.valueOf(startTimeText.getText()));
            int addonDownloadSpeed = (downloaded/(int)(currentTime - startTime));
            String localizedSpeed = Utils.formatDataFromBytes(addonDownloadSpeed*1000);

            if (finished) {
                AddonDownloadDB.removeAddonDownload(mContext, index);

                if (successful) {
                    progressBar.setProgress(0);
                    updateButtons(index, true);
                } else {
                    progressBar.setProgress(0);
                    updateButtons(index, false);
                }
            } else {
                progressBar.setProgress(progress);
                downloadSpeed.setText(localizedSpeed + "/s");
            }
        }

        public static void updateButtons(int index, boolean finished) {
            View view = mListview.getChildAt((index - 1) -
                    mListview.getFirstVisiblePosition());

            if (view == null) {
                return;
            }

            final LinearLayout download = (LinearLayout) view.findViewById(R.id.download_button);
            final LinearLayout cancel = (LinearLayout) view.findViewById(R.id.cancel_button);
            final LinearLayout delete = (LinearLayout) view.findViewById(R.id.delete_button);
            final LinearLayout progressContainer = (LinearLayout) view.findViewById(R.id.download_progress_container);
            final LinearLayout infoContainer = (LinearLayout) view.findViewById(R.id.addon_info_container);

            progressContainer.setVisibility(View.GONE);
            infoContainer.setVisibility(View.VISIBLE);

            if (finished) {
                download.setVisibility(View.GONE);
                download.setClickable(false);
                delete.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
            } else {
                download.setVisibility(View.VISIBLE);
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
            TextView progress_title = (TextView) convertView.findViewById(R.id.progress_title);
            TextView desc = (TextView) convertView.findViewById(R.id.description);
            TextView updatedOn = (TextView) convertView.findViewById(R.id.updatedOn);
            TextView filesize = (TextView) convertView.findViewById(R.id.size);
            final LinearLayout download = (LinearLayout) convertView.findViewById(R.id.download_button);
            final LinearLayout cancel = (LinearLayout) convertView.findViewById(R.id.cancel_button);
            final LinearLayout delete = (LinearLayout) convertView.findViewById(R.id.delete_button);
            final LinearLayout progressContainer = (LinearLayout) convertView.findViewById(R.id.download_progress_container);
            final LinearLayout infoContainer = (LinearLayout) convertView.findViewById(R.id.addon_info_container);
            final TextView startTime = (TextView) convertView.findViewById(R.id.start_time);
            final ImageView addonThumbnail = (ImageView) convertView.findViewById(R.id.addon_info_thumbnail);

            assert item != null;
            title.setText(item.getTitle());
            progress_title.setText(item.getTitle());

            Bypass byPass = new Bypass(mContext);
            String descriptionStr = item.getDesc();
            CharSequence string = byPass.markdownToSpannable(descriptionStr);
            desc.setText(string);
            desc.setMovementMethod(LinkMovementMethod.getInstance());

            mImageLoader.displayImage(item.getImageUrl(), addonThumbnail);

            String versionName = item.getVersionName();
            updatedOn.setText(versionName);

            filesize.setText(Utils.formatDataFromBytes(item.getFilesize()));
            final File file = new File(mContext.getExternalFilesDir(OTA_DIR_ADDONS),
                    item.getTitle() + ".zip");

            if (DEBUGGING) {
                Log.d(TAG, "file path " + file.getAbsolutePath());
                Log.d(TAG, "file length " + file.length() + " remoteLength " + item.getFilesize());
            }

            DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context
                    .DOWNLOAD_SERVICE);

            boolean finished = file.length() >= item.getFilesize();
            boolean installed = Utils.isAddonInstalled(item.getPackageName());
            boolean updated = Utils.getInstalledAddonVersion(item.getPackageName()) >= item.getVersionNumber();

            boolean downloading = false;
            long downloadId = AddonDownloadDB.getAddonDownload(mContext, item.getId());

            if (downloadId != 0) {
                downloading = true;
            }

            if (installed) {
                infoContainer.setVisibility(View.VISIBLE);
                progressContainer.setVisibility(View.GONE);
                if (updated) {
                    download.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                    cancel.setVisibility(View.GONE);
                } else {
                    download.setVisibility(View.VISIBLE);
                    download.setClickable(true);
                    cancel.setVisibility(View.GONE);
                    delete.setVisibility(View.GONE);
                }
            } else if (downloading) {
                download.setVisibility(View.GONE);
                progressContainer.setVisibility(View.VISIBLE);
                infoContainer.setVisibility(View.GONE);
                cancel.setVisibility(View.VISIBLE);
                startTime.setText(String.valueOf(System.currentTimeMillis()-1000));
                delete.setVisibility(View.GONE);
                new DownloadAddonProgress(mContext, downloadManager, item.getId(), downloadId);
            } else if (finished) {
                infoContainer.setVisibility(View.VISIBLE);
                progressContainer.setVisibility(View.GONE);
                download.setVisibility(View.GONE);
                download.setClickable(true);
                cancel.setVisibility(View.GONE);
                delete.setVisibility(View.VISIBLE);
            } else {
                progressContainer.setVisibility(View.GONE);
                download.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
                delete.setVisibility(View.GONE);
                infoContainer.setVisibility(View.VISIBLE);
            }

            download.setOnClickListener(v -> {
                boolean isMobile = Utils.isMobileNetwork(mContext);
                boolean isSettingWiFiOnly = Preferences.getNetworkType(mContext).equals
                        (WIFI_ONLY);
                long downloadIdNew = AddonDownloadDB.getAddonDownload(mContext, item.getId());

                if (isMobile && isSettingWiFiOnly) {
                    showNetworkDialog();
                } else {
                    download.setVisibility(View.GONE);
                    startTime.setText(String.valueOf(System.currentTimeMillis()));
                    progressContainer.setVisibility(View.VISIBLE);
                    infoContainer.setVisibility(View.GONE);
                    cancel.setVisibility(View.VISIBLE);
                    mDownloadAddon.startDownload(mContext, item.getDownloadLink(), item
                            .getTitle(), item.getId());
                    new DownloadAddonProgress(mContext, downloadManager, item.getId(), downloadIdNew);
                }
            });

            cancel.setOnClickListener(v -> {
                download.setVisibility(View.VISIBLE);
                progressContainer.setVisibility(View.GONE);
                infoContainer.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
                mDownloadAddon.cancelDownload(mContext, item.getId());
                updateProgress(item.getId(), 0, true, 1, false);
            });
            delete.setOnClickListener(v -> deleteConfirm(file, item));

            convertView.setOnClickListener(v -> {
                Intent i = new Intent(mContext, AddonInfoActivity.class);
                i.putExtra("id", item.getId());
                i.putExtra("name", item.getTitle());
                i.putExtra("packageName", item.getPackageName());
                i.putExtra("downloadUrl", item.getDownloadLink());
                i.putExtra("totalSize", item.getFilesize());
                i.putExtra("versionName", item.getVersionName());
                i.putExtra("fullInfo", item.getFullInfo());
                i.putExtra("versionNumber", item.getVersionNumber());
                i.putExtra("thumbnailUrl", item.getImageUrl());
                mContext.startActivity(i);
            });

            return convertView;
        }
    }

    private class LoadAddonManifest extends ArrayList<Addon> {

        private static final String MANIFEST = "addon_manifest.xml";
        public final String TAG = this.getClass().getSimpleName();
        private final ProgressDialog mLoadingDialog;
        private ArrayList<Addon> mResult;

        private final Context mContext;

        LoadAddonManifest(Context context) {
            mContext = context;

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            // Show a loading/progress dialog while the search is being performed
            mLoadingDialog = new ProgressDialog(mContext, R.style.AlertDialogStyle);
            mLoadingDialog.setIndeterminate(true);
            mLoadingDialog.setCancelable(false);
            mLoadingDialog.setMessage(mContext.getResources().getString(R.string.loading));
            mLoadingDialog.show();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    File manifest = new File(mContext.getFilesDir().getPath(), MANIFEST);
                    if (manifest.exists()) {
                        boolean deleted = manifest.delete();
                        if (!deleted) Log.e(TAG, "Unable to delete manifest file...");
                    }

                    mResult = getAddonData(mContext);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            mLoadingDialog.cancel();
                            if (mResult != null) {
                                setupListView(mResult);
                            }
                        }
                    });
                }
            });
        }

        private ArrayList<Addon> getAddonData(Context context) {

            try {
                InputStream input;

                URL url = new URL(RomUpdate.getAddonsUrl(context));
                URLConnection connection = url.openConnection();
                connection.connect();
                // download the file
                input = new BufferedInputStream(url.openStream());

                OutputStream output = context.openFileOutput(
                        MANIFEST, Context.MODE_PRIVATE);

                byte[] data = new byte[1024];
                int count;
                while ((count = input.read(data)) != -1) {
                    output.write(data, 0, count);
                }

                output.flush();
                output.close();
                input.close();

                // file finished downloading, parse it!
                AddonXmlParser parser = new AddonXmlParser();
                return parser.parse(new File(context.getFilesDir(), MANIFEST));
            } catch (Exception e) {
                Log.d(TAG, "Exception: " + e.getMessage());
            }
            return null;
        }
    }
}
