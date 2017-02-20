package se.ntlv.newsbringer.network

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.jetbrains.anko.runOnUiThread
import se.ntlv.newsbringer.thisShouldNeverHappen
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


abstract class MultithreadedIntentService : Service() {

    private class Work(val intent: Intent, val task: (Intent) -> Unit, val completion: () -> Unit) : Runnable {
        override fun run() = try {
            task(intent)
        } finally {
            completion()
        }
    }


    private val pool: ExecutorService = Executors.newCachedThreadPool()
    private val STATE_IDLE = 0
    private val STATE_DESTROYED = -1
    private val jobCounter = AtomicInteger(STATE_IDLE)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = intent.scheduleWorkWith(startId)

    private fun Intent?.scheduleWorkWith(startId: Int): Int {
        if (this != null) {
            if (jobCounter.andIncrement < 0) {
                thisShouldNeverHappen("Service was destroyed")
            }
            Log.d("ThreadedIntentService", "Submitting job, current job count is ${jobCounter.get()}")
            val work = Work(this, onBeginJob, { onComplete(startId) })
            pool.submit(work)
        }
        return START_NOT_STICKY
    }

    private fun onComplete(startId: Int) {
        Log.d("ThreadedIntentService", "Job $startId done, count before completing was ${jobCounter.get()}. Checking if we should schedule shutdown.")
        if (jobCounter.decrementAndGet() == STATE_IDLE) {
            Log.d("ThreadedIntentService", "Job $startId scheduling shutdown.")
            this.runOnUiThread {
                Log.d("ThreadedIntentService", "Job $startId on UI thread, checking if we should shutdown.")
                if (jobCounter.compareAndSet(STATE_IDLE, STATE_DESTROYED)) {
                    Log.d("ThreadedIntentService", "Job $startId shutting down service.")
                    stopSelf()
                }
            }
        }
    }

    override fun onDestroy() {
        Log.d("ThreadedIntentService", "Destroying service.")
        val counter = jobCounter.get()
        when (counter) {
            STATE_DESTROYED -> {
                val remainingWork = pool.shutdownNow()
                if (remainingWork.isNotEmpty()) {
                    val actions = remainingWork.joinToString { (it as Work).intent.toString() }
                    throw RuntimeException("Service destroyed with remaining work: $actions")
                }
            }
            else -> {
                val remainingWork = pool.shutdownNow()
                if (remainingWork.isNotEmpty()) {
                    val actions = remainingWork.joinToString { (it as Work).intent.toString() }
                    throw RuntimeException("Service destroyed while idle with remaining work: $actions")
                }
                throw RuntimeException("Service was not state destroyed while shutting down")
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = null //NO BINDING!

    abstract val onBeginJob: (Intent) -> Unit
}
