package se.ntlv.newsbringer.application

import android.app.Application
import com.google.gson.Gson
import okhttp3.Cache
import okhttp3.OkHttpClient
import se.ntlv.newsbringer.database.Database
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.LazyThreadSafetyMode.SYNCHRONIZED

object GlobalDependency {

    lateinit var app: Application

    val database by lazy(SYNCHRONIZED) { Database(app) }

    val httpClient: OkHttpClient by lazy(SYNCHRONIZED) {
        OkHttpClient.Builder()
                .cache(Cache(app.cacheDir, 10 * 1024 * 1024))
                .build()
    }

    val ioPool by lazy(SYNCHRONIZED) { ThreadPoolExecutor(0, 200, 60L, SECONDS, SynchronousQueue<Runnable>()) }

    val gson by lazy(SYNCHRONIZED) { Gson() }
}
