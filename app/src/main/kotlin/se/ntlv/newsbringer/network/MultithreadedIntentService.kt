package se.ntlv.newsbringer.network

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.WorkerThread
import org.jetbrains.anko.AnkoLogger
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


abstract class MultithreadedIntentService() : Service(), AnkoLogger {


    private val pool = Executors.newCachedThreadPool()
    private val count = AtomicInteger(0)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            count.incrementAndGet()
            pool.submit {
                onBeginJob(intent)
                if (count.decrementAndGet() == 0) {
                    stopSelf()
                }
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        val nonFinished = pool.shutdownNow()
        if (nonFinished.isNotEmpty()) {
            throw RuntimeException("Unfinished work: $nonFinished")
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    @WorkerThread
    protected abstract fun onBeginJob(intent: Intent?)
}
