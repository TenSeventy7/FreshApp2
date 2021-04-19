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
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.dlyt.yanndroid.freshapp.activities.AddonInfoActivity;
import de.dlyt.yanndroid.freshapp.utils.AddonDownloadDB;
import de.dlyt.yanndroid.freshapp.utils.Constants;

public class DownloadAddonInfoProgress implements Constants {
    public final String TAG = this.getClass().getSimpleName();

    private static final long UPDATE_DELAY = 500;
    private static long mStartTime;
    private static int mBytesDownloaded = 0;
    private static int mProgressPercent = 0;

    public DownloadAddonInfoProgress(Context context, DownloadManager downloadManager, int id, long downloadId) {
        mStartTime = System.currentTimeMillis();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            while (AddonDownloadDB.getAddonDownloading(context, id)) {
                DownloadManager.Query q = new DownloadManager.Query().setFilterById(downloadId);
                Cursor cursor = downloadManager.query(q);
                cursor.moveToFirst();
                try {
                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) ==
                            DownloadManager.STATUS_SUCCESSFUL) {

                        AddonDownloadDB.removeAddonDownloading(context, id);
                        AddonDownloadDB.removeAddonDownload(context, id);
                        AddonInfoActivity.runOnUI(() -> AddonInfoActivity.updateProgress(mProgressPercent, true, mBytesDownloaded, true));
                    } else if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) ==
                            DownloadManager.STATUS_PAUSED ||
                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) ==
                                    DownloadManager.STATUS_FAILED) {

                        AddonDownloadDB.removeAddonDownloading(context, id);
                        AddonDownloadDB.removeAddonDownload(context, id);
                        AddonInfoActivity.runOnUI(() -> AddonInfoActivity.updateProgress(mProgressPercent, true, mBytesDownloaded, false));
                    }

                    final int bytesDownloaded = cursor.getInt(cursor
                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    final int bytesInTotal = cursor.getInt(cursor
                            .getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    long currentTime = System.currentTimeMillis();

                    if ((currentTime - mStartTime > UPDATE_DELAY)) {
                        mProgressPercent = (int) ((bytesDownloaded * 100L) / bytesInTotal);
                        mBytesDownloaded = bytesDownloaded;

                        AddonInfoActivity.runOnUI(() -> AddonInfoActivity.updateProgress(mProgressPercent, false, mBytesDownloaded, false));

                        mStartTime = currentTime;
                    }
                } catch (CursorIndexOutOfBoundsException | ArithmeticException e) {
                    Log.e(TAG, " " + e.getMessage());
                }
                cursor.close();
            }
        });
    }
}
