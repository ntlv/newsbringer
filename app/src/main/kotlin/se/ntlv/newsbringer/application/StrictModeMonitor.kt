package se.ntlv.newsbringer.application

import android.app.IntentService
import android.content.Intent
import org.jetbrains.anko.AnkoLogger


class StrictModeMonitor : IntentService("StrictModeViolationReporter"), AnkoLogger {
    override fun onHandleIntent(intent: Intent?) {
//        val pid = Process.myPid()


//        var logger: java.lang.Process? = null
//        var reader: BufferedReader? = null
//        try {
//            val cmd = "logcat"
//
//            logger = Runtime.getRuntime().exec(cmd)
//            reader = logger.inputStream.bufferedReader()
//
//
//            var buffer = StringBuilder()
//            while (true){
//                val line = reader.readLine().substringAfter("StrictMode", "")
//
//                if (line.isNotEmpty()) {
//                    buffer.appendln(line)
//                } else if (buffer.isNotEmpty()){
//                    val log = buffer.toString()
//                    buffer = StringBuilder()
//                    info(log)
//                }
//            }
//        } catch (e: Exception) {
//            error("Error in Strict mode monitor", e)
//            Crashlytics.logException(e)
//        } finally {
//            reader?.close()
//            logger?.destroy()
//        }
    }
}
