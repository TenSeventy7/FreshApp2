/*
 * Copyright (C) 2017 Nicholas Chum (nicholaschum) and Matt Booth (Kryten2k35).
 *
 * Licensed under the Attribution-NonCommercial-ShareAlike 4.0 International
 * (the "License") you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://creativecommons.org/licenses/by-nc-sa/4.0/legalcode
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dlyt.yanndroid.freshapp.download;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import de.dlyt.yanndroid.freshapp.utils.Constants;
import de.dlyt.yanndroid.freshapp.utils.AddonDownloadDB;
import de.dlyt.yanndroid.freshapp.utils.Preferences;

public class DownloadAddonInfo implements Constants {

    public final static String TAG = "DownloadAddon";

    public void startDownload(Context context, String url, String fileName, int id) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

        if (Preferences.getNetworkType(context).equals(WIFI_ONLY)) {
            // All network types are enabled by default
            // So if we choose Wi-Fi only, then enable the restriction
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        }

        request.setTitle(fileName);

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        fileName = fileName + ".zip";

        // Because of Scoped Storage, we can only download into public directories.
        // Directory is '/storage/emulated/0/Download/Fresh'
        request.setDestinationInExternalFilesDir(context, OTA_DIR_ADDONS, fileName);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context
                .DOWNLOAD_SERVICE);
        long mDownloadID = downloadManager.enqueue(request);
        AddonDownloadDB.putAddonDownload(context, id, mDownloadID);
        new DownloadAddonInfoProgress(context, downloadManager, id, mDownloadID);
        if (DEBUGGING) {
            Log.d(TAG, "Starting download with manager ID " + mDownloadID + " and item id of " + id);
        }
    }

    public void cancelDownload(Context context, int id) {
        long mDownloadID = AddonDownloadDB.getAddonDownload(context, id);
        if (DEBUGGING) {
            Log.d(TAG,
                    "Stopping download with manager ID " + mDownloadID + " and item id of " + id);
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context
                .DOWNLOAD_SERVICE);
        downloadManager.remove(mDownloadID);
        AddonDownloadDB.removeAddonDownload(context, id);
    }
}
