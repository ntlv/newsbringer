package se.ntlv.newsbringer

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import se.ntlv.newsbringer.comments.CommentsActivity


class YcReaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
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
    }
}
