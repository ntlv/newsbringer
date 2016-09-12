package se.ntlv.newsbringer.database

import android.accounts.OperationCanceledException
import android.content.AsyncTaskLoader
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.CancellationSignal
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.info


class TypedCursorLoader<T : Identifiable>(
        ctx: Context,
        private val load: (TypedCursor<T>?, ContentResolver, CancellationSignal) -> TypedCursor<T>) :
        AsyncTaskLoader<TypedCursor<T>>(ctx),
        AnkoLogger {

    init {
        info { "$loggerTag initialized" }
    }

    private var cancelSignal: CancellationSignal? = null

    private val observer: ContentObserver = ForceLoadContentObserver()

    private var cursor: TypedCursor<T>? = null


    fun unsafeGetCursor() = cursor

    override fun loadInBackground(): TypedCursor<T>? {
        synchronized(this) {
            if (isLoadInBackgroundCanceled) {
                throw OperationCanceledException()
            }
            cancelSignal = CancellationSignal()
        }
        try {
            val cursor = load(cursor, context.contentResolver, cancelSignal!!)
            debug { "Creating cursor  $cursor" }
            try {
                // according to regular CursorLoader, invoke get count to make sure window is filled
                val count = cursor.count
                debug { "Cursor with $count rows created" }
                cursor.registerContentObserver(observer)
                debug { "Registering observer $observer" }
            } catch (ex: RuntimeException) {
                cursor.close()
                throw ex
            }
            return cursor
        } finally {
            synchronized(this) {
                cancelSignal = null
                debug { "Cancellation signal cleared" }
            }
        }
    }

    override fun cancelLoadInBackground() {
        debug { "Cancelling load" }
        super.cancelLoadInBackground()
        synchronized(this) {
            cancelSignal?.cancel()
        }
    }

    override fun deliverResult(data: TypedCursor<T>?) {
        debug { "Delivering result $data" }
        if (isReset) { //|| data?.isClosed ?: false) {
            cursor?.close()
            debug { "Deliverable was closed or loader reset, bailing early from delivering." }
            return
        }
        val old = cursor
        cursor = data

        if (isStarted) {
            debug { "Delivering results by invoking super method." }
            super.deliverResult(data)
        }

        if (old != null && old !== cursor && !old.isClosed) {
            debug { "Closing old deliverable" }
            old.close()
        }
    }

    override fun onStartLoading() {
        debug { "Beginning to load data." }
        cursor?.let {
            deliverResult(it)
        }
        if (takeContentChanged() || cursor === null) {
            forceLoad()
        }
    }

    override fun onStopLoading() {
        debug { "Stopping data loading." }
        cancelLoad()
    }

    override fun onCanceled(data: TypedCursor<T>?) {
        debug { "$data was cancelled" }
        if (data !== null && !data.isClosed) {
            data.close()
        }
    }

    override fun onReset() {
        debug { "Resetting" }
        super.onReset()
        onStopLoading()
        val local = cursor
        cursor = null
        if (local != null && !local.isClosed) {
            local.close()
        }
    }
}
