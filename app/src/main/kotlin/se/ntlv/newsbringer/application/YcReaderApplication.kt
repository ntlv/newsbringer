package se.ntlv.newsbringer.application

import android.app.Application
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Build
import android.os.StrictMode
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import rx.Emitter
import rx.Observable
import rx.plugins.RxJavaHooks
import rx.schedulers.Schedulers
import se.ntlv.newsbringer.BuildConfig
import se.ntlv.newsbringer.comments.CommentsActivity


class YcReaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        GlobalDependency.app = this //THIS MUST HAPPEN ASAP AFTER APP START

        var hasFabricKey = false

        try {
            val info = packageManager.getApplicationInfo(packageName, GET_META_DATA)
            hasFabricKey = info.metaData.containsKey("io.fabric.ApiKey")
        } catch (ignored: Exception) {

        }
        if (hasFabricKey) {
            Fabric.with(this, Crashlytics())
        } else {
            throw RuntimeException("No fabric key")
        }
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build())

            val builder = StrictMode.VmPolicy.Builder()
                    .setClassInstanceLimit(CommentsActivity::class.java, 4)
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.detectCleartextNetwork()
            }
            StrictMode.setVmPolicy(builder.build())
        }

        val scheduler = Schedulers.from(GlobalDependency.ioPool)
        RxJavaHooks.setOnIOScheduler { scheduler }
    }
}

fun <T> createTrackedEmitterWithAutoRemove(tracker: MutableCollection<Emitter<T>>,
                                           pressureModel: Emitter.BackpressureMode = Emitter.BackpressureMode.ERROR): Observable<T> {
    var t: Emitter<T>? = null
    return Observable.create<T>(
            {
                it.setCancellation { tracker.remove(it) }
                tracker.add(it)
                t = it
            },
            pressureModel
    ).doAfterTerminate {
        val safeT = t
        safeT?.let { tracker.remove(safeT) }

    }
}


