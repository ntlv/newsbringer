package se.ntlv.newsbringer.application

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Build
import android.os.StrictMode
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import org.jetbrains.anko.AnkoLogger
import rx.plugins.RxJavaPlugins
import rx.plugins.RxJavaSchedulersHook
import rx.schedulers.Schedulers
import se.ntlv.newsbringer.BuildConfig
import se.ntlv.newsbringer.comments.CommentsActivity


class YcReaderApplication : Application(), AnkoLogger {

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

        RxJavaPlugins.getInstance()
                .registerSchedulersHook(
                        object : RxJavaSchedulersHook() {
                            override fun getIOScheduler() = Schedulers.from(GlobalDependency.ioPool)
                        }
                )
        checkNotNull(startService(Intent(this, StrictModeMonitor::class.java)))
    }
}


