package dev.gtcl.reddit.download

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import dev.gtcl.reddit.R
import dev.gtcl.reddit.URL_KEY
import java.io.File
import java.util.*

class HlsDownloader (appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params){
    override suspend fun doWork(): Result {
        Log.d("TAE", "HlsDownloader work started")
        val base = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath)
        val uri = Uri.withAppendedPath(Uri.fromFile(base), applicationContext.getText(R.string.app_name).toString())
        val folder = File(uri.path)
        var isPresent = true
        if(!folder.exists()){
            isPresent = folder.mkdir()
        }
        if(!isPresent){
            Log.d("TAE", "Unable to create directory: ${folder.absolutePath}")
            return Result.failure()
        }
        val fileUri = Uri.withAppendedPath(Uri.fromFile(folder), "${Calendar.getInstance().timeInMillis}.mp4")
        val file = File(fileUri.path)
        val url = inputData.getString(URL_KEY)
        when(FFmpeg.execute("-i $url -acodec copy -bsf:a aac_adtstoasc -vcodec copy ${file.path}")){
            Config.RETURN_CODE_SUCCESS -> Log.d("TAE", "Executed successfully")
            Config.RETURN_CODE_CANCEL -> Log.d("TAE", "Cancelled by user.")
            else -> {
                Log.d("TAE", "ffmpeg execution failed")
                return Result.failure()
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

        Log.d("TAE", "HlsDownloader completed")

        return Result.success()
    }
}