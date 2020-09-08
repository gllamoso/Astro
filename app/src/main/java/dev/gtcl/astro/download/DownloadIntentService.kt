package dev.gtcl.astro.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.service.notification.StatusBarNotification
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import dev.gtcl.astro.ALBUM_KEY
import dev.gtcl.astro.R
import dev.gtcl.astro.URL_KEY
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.IllegalArgumentException
import kotlin.random.Random

const val HLS_EXTENSION = "m3u8"

class DownloadIntentService : JobIntentService() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(
                    NotificationChannel(
                        JOB_ID.toString(),
                        getText(R.string.download_status_notification_name),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(
                    NotificationChannel(
                        DOWNLOAD_COMPLETE_CHANNEL_ID.toString(),
                        getText(R.string.download_complete),
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
            }
        }
    }

    override fun onHandleWork(intent: Intent) {
        val externalDirectories = this.getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS)
        val picturesFolder = externalDirectories[0]
        if (picturesFolder == null) {
            Toast.makeText(
                this,
                getString(R.string.unable_to_create_external_directory),
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val saveDestination = File("$picturesFolder")
        val folderExists = createFolder(saveDestination)
        if (!folderExists) {
            val error = String.format(
                getString(R.string.unable_to_create_directory),
                saveDestination.absolutePath
            )
            Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            stopForeground(false)
            return
        }

        val downloadUrl = intent.extras!![URL_KEY] as String?
        if (downloadUrl != null) { // Download single item
            val filename = createFileName(downloadUrl)
            startForeground(JOB_ID, createForegroundNotificationForSingleItem(filename))
            val uri = downloadItem(downloadUrl, saveDestination)
            if (uri != null) {
                showDownloadCompleteNotificationForSingleItem(uri, filename)
            }
            stopForeground(true)
            stopSelf()
        } else { // Download album
            val urls = intent.extras!![ALBUM_KEY] as List<*>
            val notification = createForegroundNotificationForAlbum()
            startForeground(JOB_ID, notification.build())
            var itemsComplete = 0
            urls.forEach { url: Any? ->
                if (url is String) {
                    downloadItem(url, saveDestination)
                    itemsComplete++
                    val completionPercentage = ((itemsComplete.toDouble()) / urls.size) * 100
                    notification.setProgress(100, completionPercentage.toInt(), false)
                    NotificationManagerCompat.from(this).notify(JOB_ID, notification.build())
                }
            }
            showDownloadCompleteNotificationForAlbum()
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createForegroundNotificationForSingleItem(fileName: String): Notification {
        return NotificationCompat.Builder(this, JOB_ID.toString())
            .setContentTitle(fileName)
            .setSmallIcon(R.drawable.ic_download_24)
            .setContentText(getText(R.string.downloading))
            .setProgress(100, 0, true)
            .build()
    }

    private fun createForegroundNotificationForAlbum(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, JOB_ID.toString())
            .setContentTitle(getString(R.string.downloading_album))
            .setSmallIcon(R.drawable.ic_download_24)
            .setProgress(100, 0, false)
    }

    private fun createItemCompleteNotification(uri: Uri, filename: String): Notification {
        val fileExtension = getExtension(filename)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = when (fileExtension) {
                "mp4", "wav" -> "video/*"
                "jpeg", "bmp", "gif", "jpg", "png" -> "image/*"
                else -> throw IllegalArgumentException("Invalid file type: $fileExtension")
            }
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            data = uri
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        return NotificationCompat.Builder(this, DOWNLOAD_COMPLETE_CHANNEL_ID.toString())
            .setContentIntent(pendingIntent)
            .setContentTitle(filename)
            .setSmallIcon(R.drawable.ic_save_24)
            .setContentText(getText(R.string.download_complete))
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setAutoCancel(true)
            .build()
    }

    private fun createAlbumCompleteNotification(): Notification {
        return NotificationCompat.Builder(this, DOWNLOAD_COMPLETE_CHANNEL_ID.toString())
            .setSmallIcon(R.drawable.ic_save_24)
            .setContentText(getText(R.string.album_download_complete))
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setAutoCancel(true)
            .build()
    }

    private fun showDownloadCompleteNotificationForSingleItem(uri: Uri, filename: String) {
        val uniqueId = Random.nextInt() + DOWNLOAD_COMPLETE_CHANNEL_ID
        val newNotification = createItemCompleteNotification(uri, filename)
        NotificationManagerCompat.from(this).notify(uniqueId, newNotification)

        var count = 0
        for (notification: StatusBarNotification in (application.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager).activeNotifications) {
            if (notification.notification.group == newNotification.group) {
                count++
            }
        }

        if (count > 1) {
            val summaryNotification =
                NotificationCompat.Builder(this, DOWNLOAD_COMPLETE_CHANNEL_ID.toString())
                    .setSmallIcon(R.drawable.ic_save_24)
                    .setGroup(DOWNLOAD_GROUP_KEY)
                    .setGroupSummary(true)
                    .build()

            NotificationManagerCompat.from(this).notify(SUMMARY_ID, summaryNotification)
        }
    }

    private fun showDownloadCompleteNotificationForAlbum() {
        val uniqueId = Random.nextInt() + DOWNLOAD_COMPLETE_CHANNEL_ID
        val newNotification = createAlbumCompleteNotification()
        NotificationManagerCompat.from(this).notify(uniqueId, newNotification)

        var count = 0
        for (notification: StatusBarNotification in (application.getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager).activeNotifications) {
            if (notification.notification.group == newNotification.group) {
                count++
            }
        }

        if (count > 1) {
            val summaryNotification =
                NotificationCompat.Builder(this, DOWNLOAD_COMPLETE_CHANNEL_ID.toString())
                    .setSmallIcon(R.drawable.ic_save_24)
                    .setGroup(DOWNLOAD_GROUP_KEY)
                    .setGroupSummary(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

            NotificationManagerCompat.from(this).notify(SUMMARY_ID, summaryNotification)
        }
    }

    private fun createFolder(folder: File): Boolean {
        if (!folder.exists()) {
            return folder.mkdir()
        }
        return true
    }

    private fun downloadStandardFile(url: String, savePath: String) {
        val downloadUrl = URL(url)
        val connection = downloadUrl.openConnection()
//        val fileLength = connection.contentLength
        val input = BufferedInputStream(connection.getInputStream())
        val output = FileOutputStream(savePath)

        try {
            val data = ByteArray(1024)
            var total = 0L
            var count = input.read(data)
            while (count != -1) {
                total += count
//                val progress = total * 100 / fileLength
                output.write(data, 0, count)
                count = input.read(data)
            }
        } catch (e: IOException) {
            Timber.tag(TAG).e("IOException $e")
        } finally {
            output.flush()
            output.close()
            input.close()
        }
    }

    private fun getExtension(filename: String) =
        filename.replace("[a-zA-Z\\-0-9]+\\.".toRegex(), "")

    private fun createFileName(url: String): String {
        val fileExtension = getExtension(Uri.parse(url).lastPathSegment!!)
        return if (fileExtension.equals(HLS_EXTENSION, true)) {
            "${Calendar.getInstance().timeInMillis}.mp4"
        } else {
            Uri.parse(url).lastPathSegment!!
        }
    }

    private fun downloadItem(url: String, saveDestination: File): Uri? {
        val filename = createFileName(url)
        val fileUri = Uri.withAppendedPath(Uri.fromFile(saveDestination), filename)
        val file = File(fileUri.path!!)
        val fileExtension = getExtension(Uri.parse(url).lastPathSegment!!)

        if (fileExtension.toLowerCase(Locale.getDefault()) == HLS_EXTENSION) { // For downloading HLS videos
            if (FFmpeg.execute("-i $url -acodec copy -bsf:a aac_adtstoasc -vcodec copy ${file.path}") != Config.RETURN_CODE_SUCCESS) {
                Timber.tag(TAG).e("ffmpeg execution failed")
                stopForeground(true)
                val notification = NotificationCompat.Builder(this, JOB_ID.toString())
                    .setContentTitle(getString(R.string.unable_to_download_file))
                    .setContentText(filename)
                    .setSmallIcon(R.drawable.ic_error_outline_24)
                    .build()
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(FAILURE_ID, notification)
                return null
            }
        } else {
            downloadStandardFile(url, fileUri.path!!)
        }

        // Move file to MediaStore
        val mimeType = when (file.extension) {
            "mp4", "wav" -> "video/*"
            "jpeg", "bmp", "gif", "jpg", "png" -> "image/*"
            else -> throw IllegalArgumentException("Invalid file type: $fileExtension")
        }
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val folder = when (mimeType) {
                    "video/*" -> "Movies"
                    else -> "DCIM"
                }
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "$folder/${getString(R.string.app_name)}"
                )
            }
        }
        val externalUri = if (mimeType == "video/*") {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = contentResolver.insert(externalUri, contentValues)
        contentResolver.openOutputStream(uri!!).use {
            val test = file.readBytes()
            if (it != null) {
                it.write(test)
                it.close()
                file.delete()
            }
        }

        return uri
    }

    companion object {
        private const val JOB_ID = 1001
        private const val DOWNLOAD_COMPLETE_CHANNEL_ID = 2000
        private const val FAILURE_ID = 3000
        private const val SUMMARY_ID = 1999
        private val DOWNLOAD_GROUP_KEY = DownloadIntentService::class.java.name
        val TAG: String = DownloadIntentService::class.java.simpleName
        fun enqueueWork(context: Context, downloadUrl: String) {
            val serviceIntent = Intent(context, DownloadIntentService::class.java)
            serviceIntent.putExtra(URL_KEY, downloadUrl)
            enqueueWork(context, DownloadIntentService::class.java, JOB_ID, serviceIntent)
        }

        fun enqueueWork(context: Context, downloadUrls: List<String>) {
            val serviceIntent = Intent(context, DownloadIntentService::class.java)
            serviceIntent.putStringArrayListExtra(ALBUM_KEY, ArrayList(downloadUrls))
            enqueueWork(context, DownloadIntentService::class.java, JOB_ID, serviceIntent)
        }
    }

}