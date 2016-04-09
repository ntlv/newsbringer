package se.ntlv.newsbringer

import android.app.Application
import android.os.StrictMode
import org.jetbrains.anko.AnkoLogger


class YcReaderApplication : Application(), AnkoLogger {

    override fun onCreate() {
        super.onCreate()
//        Fabric.with(this, Crashlytics());
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build())

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectCleartextNetwork()
                    .detectLeakedRegistrationObjects()
                    .detectLeakedSqlLiteObjects()
                    //                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build())
        }
    }
}
