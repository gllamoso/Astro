package dev.gtcl.reddit.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import dev.gtcl.reddit.R
import dev.gtcl.reddit.URL_KEY
import kotlinx.coroutines.*
import java.io.File
import java.util.*

class DownloadService : Service(){

    private var job = Job()
    private var coroutineScope = CoroutineScope(job + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent == null) return START_STICKY_COMPATIBILITY
        coroutineScope.launch {
            withContext(Dispatchers.IO){
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                        createNotificationChannel(NotificationChannel(TAG, getText(R.string.download_status_notification_name), NotificationManager.IMPORTANCE_LOW))
                    }
                }
                val notification = Notification.Builder(this@DownloadService, TAG)
                    .setContentTitle("Title")
                    .setSmallIcon(R.drawable.ic_reddit_circle)
                    .setContentText("Test $TEST")
                    .build()

                val randomId = TEST++
                Log.d("TAE", "Random Id: $randomId")
                startForeground(randomId, notification)

                val base = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath)
                val uri = Uri.withAppendedPath(Uri.fromFile(base), applicationContext.getText(R.string.app_name).toString())
                val folder = File(uri.path)
                var isPresent = true
                if(!folder.exists()){
                    isPresent = folder.mkdir()
                }
                if(!isPresent){
                    Log.d(TAG, "Unable to create directory: ${folder.absolutePath}")
                    stopForeground(false)
                    stopSelf()
                    return@withContext
                }
                val fileUri = Uri.withAppendedPath(Uri.fromFile(folder), "${Calendar.getInstance().timeInMillis}.mp4")
                val file = File(fileUri.path)
                val url = intent.extras!![URL_KEY]
                when(FFmpeg.execute("-i $url -acodec copy -bsf:a aac_adtstoasc -vcodec copy ${file.path}")){
                    Config.RETURN_CODE_SUCCESS -> Log.d("TAE", "Executed successfully")
                    else -> {
                        Log.d(TAG, "ffmpeg execution failed")
                        stopForeground(false)
                        stopSelf()
                        return@withContext
                    }
                }

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
                stopForeground(false)
                stopSelf()
                Log.d("TAE", "Stopped Service")
            }
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("TAE" , "Service: onTaskRemoved")
    }

    override fun onDestroy() {
        Log.d("TAE", "Service: OnDestroy")
    }

    companion object{
        val TAG: String = DownloadService::class.java.simpleName
        var TEST = 1
    }
}