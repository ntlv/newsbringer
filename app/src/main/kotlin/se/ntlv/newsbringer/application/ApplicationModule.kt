package se.ntlv.newsbringer.application

import android.app.Application
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import okhttp3.Cache
import okhttp3.OkHttpClient
import se.ntlv.newsbringer.database.Database
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
class ApplicationModule(private val app: Application) {

    @Provides
    @Singleton
    fun provideApplication(): Application = app

    @Provides
    @Singleton
    fun providePostsDb(): Database = Database(app)

    @Provides
    @Named("cache")
    fun providesCacheDir(): File = app.cacheDir

    @Provides
    @Named("version")
    fun providesAppVersion(): Int = app.packageManager.getPackageInfo(app.packageName, 0).versionCode


    @Provides
    @Singleton
    fun providesHttpClient(@Named("cache") cache: File): OkHttpClient =
            OkHttpClient.Builder()
                    .cache(Cache(cache, 10 * 1024 * 1024))
                    .build()

    @Provides
    @Singleton
    fun providesGson() : Gson = Gson()
}