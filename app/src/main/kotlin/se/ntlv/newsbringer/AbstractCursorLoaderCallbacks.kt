package se.ntlv.newsbringer

import android.widget.ResourceCursorAdapter
import android.app.LoaderManager
import android.database.Cursor
import android.content.Loader
import android.os.Bundle

trait AbstractCursorLoaderCallbacks : LoaderManager.LoaderCallbacks<Cursor> {
    val mAdapter : ResourceCursorAdapter
    fun getOnLoadFinishedCallback(): ((t: Cursor?) -> Unit)? = null
    fun getOnLoaderResetCallback(): ((t: Loader<Cursor>?) -> Unit)? = null

    override fun onCreateLoader(id: Int, args: Bundle?) : Loader<Cursor>

    override fun onLoadFinished(loader: Loader<Cursor>?, cursor: Cursor?) {
        mAdapter.swapCursor(cursor)
        getOnLoadFinishedCallback()?.invoke(cursor)
    }

    override fun onLoaderReset(loader: Loader<Cursor>?) {
        mAdapter.swapCursor(null)
        getOnLoaderResetCallback()?.invoke(loader)
    }
}
