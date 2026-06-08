package eco.humanos.android.core.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

/**
 * Updates the app entirely from the phone — no PC, no manual file transfer.
 *
 * [downloadAndInstall] pulls the GitHub release APK via Android's
 * [DownloadManager] (shows a download notification), then, on completion,
 * launches the system package installer through a [FileProvider] URI so the
 * user just taps "Install". Everything is best-effort: any failure falls back
 * to opening the download URL in the browser (the previous behaviour), so the
 * user is never left stuck.
 *
 * Requires (declared in the app manifest):
 *  - `REQUEST_INSTALL_PACKAGES` permission (Android prompts the user to allow
 *    "install unknown apps" the first time).
 *  - A `FileProvider` with authority `${applicationId}.fileprovider`.
 */
object ApkInstaller {

    private const val APK_MIME = "application/vnd.android.package-archive"

    fun downloadAndInstall(context: Context, url: String, versionName: String) {
        val appContext = context.applicationContext
        val fileName = "humanOS-$versionName.apk"

        val ok = runCatching {
            val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // App-specific external dir → no storage permission needed.
            File(appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                .takeIf { it.exists() }?.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("humanOS $versionName")
                .setDescription("Descargando actualización…")
                .setMimeType(APK_MIME)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
                )
                .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, fileName)

            val downloadId = dm.enqueue(request)
            Toast.makeText(appContext, "Descargando v$versionName…", Toast.LENGTH_SHORT).show()

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                    if (id != downloadId) return
                    runCatching { appContext.unregisterReceiver(this) }
                    val file = File(
                        appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        fileName,
                    )
                    if (!(file.exists() && installApk(appContext, file))) {
                        openInBrowser(appContext, url)
                    }
                }
            }
            ContextCompat.registerReceiver(
                appContext,
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                ContextCompat.RECEIVER_EXPORTED,
            )
        }.isSuccess

        if (!ok) openInBrowser(appContext, url)
    }

    /** Launch the system installer for [file]. Returns false if it couldn't start. */
    private fun installApk(context: Context, file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    private fun openInBrowser(context: Context, url: String) {
        if (url.isBlank()) return
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
