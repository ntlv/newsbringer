package se.ntlv.newsbringer.network

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.WorkerThread
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


abstract class MultithreadedIntentService() : Service(), AnkoLogger {


    private val pool = Executors.newCachedThreadPool()
    private val count = AtomicInteger(0)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            debug { "SUBMITTING: ${intent.action}, ${intent.extras.get("id") ?: "No id"}" }
            count.incrementAndGet()
            pool.submit {
                debug { "BEGINNING: ${intent.action}, ${intent.extras.get("id") ?: "No id"}" }
                onBeginJob(intent)
                debug {"DONE: ${intent.action}, ${intent.extras.get("id") ?: "No id"}"}
                val localCount = count.decrementAndGet()
                if (localCount == 0) {
                    debug { "STOPPING" }
                    stopSelf()
                } else {
                    debug { "NOT STOPPING: $localCount " }
                }
            }
        }
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        debug { "Destroying $this" }
        val nonFinished = pool.shutdownNow()
        if (nonFinished.isNotEmpty()) {
            throw RuntimeException("Unfinished work: $nonFinished")
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    @WorkerThread
    protected abstract fun onBeginJob(intent: Intent?)
}