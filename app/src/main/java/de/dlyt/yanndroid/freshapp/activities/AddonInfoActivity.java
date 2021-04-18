package de.dlyt.yanndroid.freshapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.AppBarLayout;
import com.nostra13.universalimageloader.core.ImageLoader;

import de.dlyt.yanndroid.freshapp.R;
import in.uncod.android.bypass.Bypass;

public class AddonInfoActivity extends AppCompatActivity {

    public static Context mContext;
    public static ImageLoader mImageLoader;
    public static TextView mDownloadedSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addon_info);
        mImageLoader = ImageLoader.getInstance();

        initToolbar();

        TextView expanded_subtitle = findViewById(R.id.expanded_subtitle);
        expanded_subtitle.setText("");

        final ImageView addonThumbnail = (ImageView) findViewById(R.id.addon_info_thumbnail);
        final TextView addonVersion = (TextView) findViewById(R.id.version_number);
        final TextView addonPackageName = (TextView) findViewById(R.id.package_name);
        final TextView addonFullInfo = (TextView) findViewById(R.id.addon_information_description);
        final TextView addonTotalSize = (TextView) findViewById(R.id.addon_download_total);
        final TextView addonName = (TextView) findViewById(R.id.title);
        mDownloadedSize = (TextView) findViewById(R.id.addon_download_size);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        String name = intent.getStringExtra("name");
        String packageName = intent.getStringExtra("packageName");
        String downloadUrl = intent.getStringExtra("downloadUrl");
        String totalSize = intent.getStringExtra("totalSize");
        String versionName = intent.getStringExtra("versionName");
        String fullInfo = intent.getStringExtra("fullInfo");
        String versionNumber = intent.getStringExtra("versionNumber");
        String thumbnailUrl = intent.getStringExtra("thumbnailUrl");

        mImageLoader.displayImage(thumbnailUrl, addonThumbnail);
        addonVersion.setText(versionName);
        addonPackageName.setText(packageName);
        addonTotalSize.setText(totalSize);
        addonName.setText(name);
        settilte(" ");

        Bypass byPass = new Bypass(mContext);
        CharSequence string = byPass.markdownToSpannable(fullInfo);
        addonFullInfo.setText(string);
        addonFullInfo.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void initToolbar() {
        /** Def */
        androidx.appcompat.widget.Toolbar toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
}