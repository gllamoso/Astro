package dev.gtcl.reddit.download

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import dev.gtcl.reddit.R
import dev.gtcl.reddit.URL_KEY
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.*
import kotlin.IllegalArgumentException
import kotlin.random.Random

const val HLS_EXTENSION = "m3u8"
class DownloadIntentService : JobIntentService(){

    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(NotificationChannel(JOB_ID.toString(), getText(R.string.download_status_notification_name), NotificationManager.IMPORTANCE_LOW))
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(NotificationChannel(DOWNLOAD_COMPLETE_CHANNEL_ID.toString(), getText(R.string.download_complete), NotificationManager.IMPORTANCE_LOW))
            }
        }
    }

    @SuppressLint("DefaultLocale")
    override fun onHandleWork(intent: Intent) {

        val url = intent.extras!![URL_KEY] as String
        val fileExtension = getExtension(Uri.parse(url).lastPathSegment!!)
        val fileName: String =
            if(fileExtension.toLowerCase() == HLS_EXTENSION) {
                "${Calendar.getInstance().timeInMillis}.mp4"
            } else {
                Uri.parse(url).lastPathSegment!!
            }
        startForeground(JOB_ID, createForegroundNotification(fileName))

        val picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
        val saveDestination = File("${picturesFolder}/${applicationContext.getText(R.string.app_name)}")
        val folderExists = createFolder(saveDestination)
        if(!folderExists){
            val text = String.format(getString(R.string.unable_to_create_directory), saveDestination.absolutePath)
            Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show() // TODO: Add String value
            stopForeground(false)
            return
        }

        val fileUri = Uri.withAppendedPath(Uri.fromFile(saveDestination), fileName)
        val file = File(fileUri.path!!)

        if(fileExtension.toLowerCase() == HLS_EXTENSION){ // For downloading HLS videos
            if(FFmpeg.execute("-i $url -acodec copy -bsf:a aac_adtstoasc -vcodec copy ${file.path}")
                != Config.RETURN_CODE_SUCCESS){
                Log.d(TAG, "ffmpeg execution failed")
                stopForeground(true)
                // TODO: Create failure notification
                return
            }
        } else {
            downloadStandardFile(url, fileUri.path!!)
        }

        scanMedia(file)
        showDownloadCompleteNotification(file.absolutePath)

        stopForeground(true)
    }

    private fun createForegroundNotification(fileName: String): Notification{
        return NotificationCompat.Builder(this, JOB_ID.toString())
            .setContentTitle(fileName)
            .setSmallIcon(R.drawable.ic_download_black_24)
            .setContentText(getText(R.string.downloading))
            .setProgress(100, 0, true)
            .build()
    }

    private fun createDownloadCompleteNotification(filePath: String): Notification{

        val file = Uri.parse(filePath)
        val fileName = file.lastPathSegment!!
        val fileExtension = getExtension(fileName)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = when(fileExtension){
                "mp4","wav" -> "video/*"
                "jpeg", "bmp", "gif", "jpg", "png" -> "image/*"
                else -> throw IllegalArgumentException("Invalid file type: $fileExtension") // TODO: Fix Gfycat download errors
            }
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            data = FileProvider.getUriForFile(this@DownloadIntentService, this@DownloadIntentService.applicationContext.packageName + ".provider", File(filePath))
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        return NotificationCompat.Builder(this, DOWNLOAD_COMPLETE_CHANNEL_ID.toString())
            .setContentIntent(pendingIntent)
            .setContentTitle(fileName)
            .setSmallIcon(R.drawable.ic_save_24)
            .setContentText(getText(R.string.download_complete))
            .setGroup(DOWNLOAD_GROUP_KEY)
            .setAutoCancel(true)
            .build()
    }

    private fun showDownloadCompleteNotification(filePath: String){
        val uniqueId = Random.nextInt() + DOWNLOAD_COMPLETE_CHANNEL_ID
        val newNotification = createDownloadCompleteNotification(filePath)
        NotificationManagerCompat.from(this).notify(uniqueId, newNotification)

//        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        var count = 0
        for(notification: StatusBarNotification in (application.getSystemService(NOTIFICATION_SERVICE) as NotificationManager).activeNotifications){
            if(notification.notification.group == newNotification.group){
                count++
            }
        }

        if(count > 1){
            val summaryNotification = NotificationCompat.Builder(this, DOWNLOAD_COMPLETE_CHANNEL_ID.toString())
                .setSmallIcon(R.drawable.ic_save_24)
                .setGroup(DOWNLOAD_GROUP_KEY)
                .setGroupSummary(true)
                .build()

            NotificationManagerCompat.from(this).notify(SUMMARY_ID, summaryNotification)
        }
    }

    private fun createFolder(folder: File): Boolean{
        if(!folder.exists()){
            return folder.mkdir()
        }
        return true
    }

    private fun downloadStandardFile(url: String, savePath: String){
        val downloadUrl = URL(url)
        val connection = downloadUrl.openConnection()
//        val fileLength = connection.contentLength
        val input = BufferedInputStream(connection.getInputStream())
        val output = FileOutputStream(savePath)

        try {
            val data = ByteArray(1024)
            var total = 0L
            var count = input.read(data)
            while(count != -1){
                total += count
//                val progress = total * 100 / fileLength
                output.write(data, 0, count)
                count = input.read(data)
            }
        } catch(e: IOException){
            Log.d(TAG, "IOException: $e")
        } finally {
            output.flush()
            output.close()
            input.close()
        }
    }

    private fun scanMedia(file: File){
        val client = object: MediaScannerConnection.MediaScannerConnectionClient{
            private val path = file.absolutePath
            lateinit var connection: MediaScannerConnection
            override fun onMediaScannerConnected() {
                connection.scanFile(path, null)
            }

            override fun onScanCompleted(path: String?, uri: Uri?) {
                connection.disconnect()
            }
        }
        val connection = MediaScannerConnection(applicationContext, client)
        client.connection = connection
        connection.connect()
    }

    private fun getExtension(fileName: String) =
        fileName.replace("[A-Za-z0-9]+\\.".toRegex(), "").toLowerCase(Locale.getDefault())

    companion object {
        private const val JOB_ID = 1001
        private const val DOWNLOAD_COMPLETE_CHANNEL_ID = 2000
        private const val SUMMARY_ID = 1999
        private val DOWNLOAD_GROUP_KEY = DownloadIntentService::class.java.name
        val TAG: String = DownloadIntentService::class.java.simpleName
        fun enqueueWork(context: Context, downloadUrl: String){
            val serviceIntent = Intent(context, DownloadIntentService::class.java)
            serviceIntent.putExtra(URL_KEY, downloadUrl)
            enqueueWork(context, DownloadIntentService::class.java, JOB_ID, serviceIntent)
        }

        fun enqueueWork(context: Context, downloadUrls: List<String>, albumName: String){
            TODO("Implement album download")
//            val serviceIntent = Intent(context, DownloadIntentService::class.java)
//            serviceIntent.putExtra(ALBUM_KEY, albumName)
//            serviceIntent.putStringArrayListExtra(URLS_KEY, ArrayList<String>(downloadUrls))
//            enqueueWork(context, DownloadIntentService::class.java, JOB_ID, serviceIntent)
        }
    }

}