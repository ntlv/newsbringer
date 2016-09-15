package se.ntlv.newsbringer.application

import android.app.Application
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Build
import android.os.StrictMode
import se.ntlv.newsbringer.BuildConfig
import se.ntlv.newsbringer.comments.CommentsActivity


class YcReaderApplication : Application() {

    companion object {
        private lateinit var graph: ApplicationComponent

        fun applicationComponent() = graph
    }

    override fun onCreate() {
        super.onCreate()
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
                    .penaltyDeath()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.detectCleartextNetwork()
            }
            StrictMode.setVmPolicy(builder.build())
        }
        graph = ApplicationComponent.init(ApplicationModule(this))
    }
}


