package se.ntlv.newsbringer.network

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.support.annotation.WorkerThread
import se.ntlv.newsbringer.application.GlobalDependency
import se.ntlv.newsbringer.thisShouldNeverHappen
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicInteger


abstract class MultithreadedIntentService : Service() {

    private val pool: ThreadPoolExecutor = GlobalDependency.ioPool
    private val STATE_IDLE = 0
    private val STATE_DESTROYED = -1
    private val jobCounter = AtomicInteger(STATE_IDLE)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {
            if (jobCounter.incrementAndGet() == STATE_DESTROYED) {
                thisShouldNeverHappen()
            }
            pool.submit {
                onBeginJob(intent)
                if (jobCounter.decrementAndGet() == STATE_IDLE) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        if (!jobCounter.compareAndSet(STATE_IDLE, STATE_DESTROYED)) {
            val counter = jobCounter.get()
            throw RuntimeException("Service was not idle when destroyed, work remaining: $counter")
        }
    }

    override fun onBind(intent: Intent): IBinder? = null //NO BINDING!

    @WorkerThread
    protected abstract fun onBeginJob(intent: Intent)
}
