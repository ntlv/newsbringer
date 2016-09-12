package se.ntlv.newsbringer

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

interface UiControlService {
    fun ping()
}


class UiControlServiceImpl : Service(), UiControlService {

    val binder: LocalBinder by lazy { LocalBinder(this) }

    override fun onBind(intent: Intent): IBinder? = binder

    class LocalBinder(val service: UiControlService) : Binder() {

        fun getInterface(): UiControlService {
            return service
        }
    }

    private val TAG = UiControlServiceImpl::class.java.simpleName

    override fun ping() {
        Log.i(TAG, "Pinged by client")
    }
}

class UiControlClient(context: Context) {

    private val TAG = UiControlClient::class.java.simpleName

    private var service: UiControlService? = null
    private val context = context.applicationContext

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            this@UiControlClient.service = (service as UiControlServiceImpl.LocalBinder).getInterface()
            val iter = callQueue.iterator()
            while (iter.hasNext()) {
                val call = iter.next()
                iter.remove()
                Log.i(TAG, "Invoking enqueued call $call")
                call()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            callQueue.clear()
        }
    }

    fun connect() {
        service?.let { throw IllegalStateException("Already connected.") }
        val intent = Intent(context, UiControlServiceImpl::class.java)
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            throw IllegalStateException("Unable to connect to UI Service")
        }
    }

    fun disconnect() {
        service?.let {
            service = null
            context.unbindService(connection)
            callQueue.clear()
        }
    }

    private val callQueue = ArrayList<() -> Unit>()

    fun ping() {
        service?.ping() ?: callQueue.add { ping() }
    }
}
