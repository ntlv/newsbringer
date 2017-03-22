package se.ntlv.newsbringer.network

import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ThrowingExecutor(corePoolSize: Int = 0,
                       maxPoolSize: Int = 200,
                       keepAliveTime: Long = 60L,
                       unit: TimeUnit = TimeUnit.SECONDS,
                       workQueue: SynchronousQueue<Runnable> = SynchronousQueue()) :

        ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveTime, unit, workQueue) {

    override fun afterExecute(r: Runnable?, t: Throwable?) = when {
        t != null -> throw RuntimeException(t)
        else -> super.afterExecute(r, t)
    }
}