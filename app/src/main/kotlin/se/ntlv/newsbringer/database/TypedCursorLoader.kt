package se.ntlv.newsbringer.database

import android.accounts.OperationCanceledException
import android.content.AsyncTaskLoader
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.CancellationSignal
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.error


abstract class TypedCursorLoader<T>(ctx: Context) : AsyncTaskLoader<TypedCursor<T>>(ctx), AnkoLogger {

    abstract fun query(resolver: ContentResolver, signal: CancellationSignal?): TypedCursor<T>?

    private var cancelSignal: CancellationSignal? = null

    private val observer: ContentObserver = ForceLoadContentObserver()

    private var cursor: TypedCursor<T>? = null

    override fun loadInBackground(): TypedCursor<T>? {
        debug("loadInBackground()")
        synchronized(this) {
            debug("doing synchronized cancel check")
            if (isLoadInBackgroundCanceled) {
                debug("isLoadingInBackgroundCancel == true, throwing")
                throw OperationCanceledException()
            }
            cancelSignal = CancellationSignal()
        }
        try {
            debug("Attempting to load ...")
            val cursor = query(context.contentResolver, cancelSignal)
            debug("got cursor { $cursor }")
            if (cursor != null) {
                try {
                    val count = cursor.count
                    debug("Cursor count {$count}, registering observer")
                    cursor.registerContentObserver(observer)
                } catch (ex: RuntimeException) {
                    cursor.close()
                    throw ex
                }
            }
            debug("Returning cursor {$cursor} from background load")
            return cursor
        } catch (ex : Exception) {
            error("Error while loading cursor", ex)
            throw ex
        } finally {
            debug("Attempting synchronized background cancel signal release")
            synchronized(this) {
                cancelSignal = null
                debug("Successfully release cancel signal")
            }
        }
    }

    override fun cancelLoadInBackground() {
        debug("cancelLoadInBackground()")
        super.cancelLoadInBackground()
        synchronized(this) {
            debug("cancelLoadInBackground, sending cancel signal to cursor")
            cancelSignal?.cancel()
        }
    }

    override fun deliverResult(data: TypedCursor<T>?) {
        debug("deliverResult(data = $data)")
        if (isReset) {
            cursor?.close()
            debug("Was reset, closing cursor and bailing.")
            return
        }
        val old = cursor
        cursor = data

        if (isStarted) {
            super.deliverResult(data)
        }

        if (old != null && old !== cursor && !old.isClosed) {
            old.close()
        }
    }

    override fun onStartLoading() {
        debug("onStartLoading()")

        cursor?.let {
            deliverResult(it)
        }
        if (takeContentChanged() || cursor == null) {
            forceLoad()
        }
    }

    override fun onStopLoading() {
        cancelLoad()
    }

    override fun onCanceled(data: TypedCursor<T>?) {
        debug("onCanceled(data = $data)")

        if (data != null && !data.isClosed) {
            data.close()
        }
    }

    private val TAG = javaClass.simpleName

    override fun onReset() {

        debug("onReset()")

        super.onReset()
        onStopLoading()
        val local = cursor
        cursor = null
        if (local != null && !local.isClosed) {
            local.close()
        }
    }
}