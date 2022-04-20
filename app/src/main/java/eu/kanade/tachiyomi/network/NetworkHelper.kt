package eu.kanade.tachiyomi.network

import android.content.Context
import coil.util.CoilUtils
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.TimeUnit

class NetworkHelper(val context: Context) {

    private val preferences: PreferencesHelper by injectLazy()

    private val cacheDir = File(context.cacheDir, "network_cache")

    private val cacheSize = 5L * 1024 * 1024 // 5 MiB

    val cookieManager = AndroidCookieJar()

    private val baseClientBuilder: OkHttpClient.Builder
        get() {
            val builder = OkHttpClient.Builder()
                .cookieJar(cookieManager)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(2, TimeUnit.MINUTES)
                .addInterceptor(UserAgentInterceptor())
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(
                            ChuckerInterceptor.Builder(context)
                                .collector(ChuckerCollector(context))
                                .maxContentLength(250000L)
                                .redactHeaders(emptySet())
                                .alwaysReadResponseBody(false)
                                .build()
                        )
                    }

                    when (preferences.dohProvider()) {
                        PREF_DOH_CLOUDFLARE -> dohCloudflare()
                        PREF_DOH_GOOGLE -> dohGoogle()
                        PREF_DOH_ADGUARD -> dohAdGuard()
                        PREF_DOH_QUAD9 -> dohQuad9()
                    }
                }

            return builder
        }

    val client by lazy { baseClientBuilder.cache(Cache(cacheDir, cacheSize)).build() }

    val coilClient by lazy { baseClientBuilder.cache(CoilUtils.createDefaultCache(context)).build() }

    val cloudflareClient by lazy {
        client.newBuilder()
            .addInterceptor(CloudflareInterceptor(context))
            .build()
    }
}
