package eu.kanade.tachiyomi.extension.api

import android.content.Context
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.LoadResult
import eu.kanade.tachiyomi.extension.util.ExtensionLoader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import uy.kohesive.injekt.injectLazy

internal class ExtensionGithubApi {

    private val networkService: NetworkHelper by injectLazy()

    suspend fun findExtensions(): List<Extension.Available> {
        return withIOContext {
            networkService.client
                .newCall(GET("${REPO_URL_PREFIX}index.min.json"))
                .await()
                .parseAs<JsonArray>()
                .let { parseResponse(it) }
        }
    }

    suspend fun checkForUpdates(context: Context, prefetchedExtensions: List<Extension.Available>? = null): List<Extension.Available> {
        return withIOContext {
            val extensions = prefetchedExtensions ?: findExtensions()

            val installedExtensions = ExtensionLoader.loadExtensions(context)
                .filterIsInstance<LoadResult.Success>()
                .map { it.extension }

            val extensionsWithUpdate = mutableListOf<Extension.Available>()
            for (installedExt in installedExtensions) {
                val pkgName = installedExt.pkgName
                val availableExt = extensions.find { it.pkgName == pkgName } ?: continue

                val hasUpdate = availableExt.versionCode > installedExt.versionCode
                if (hasUpdate) {
                    extensionsWithUpdate.add(availableExt)
                }
            }

            extensionsWithUpdate
        }
    }

    private fun parseResponse(json: JsonArray): List<Extension.Available> {
        return json
            .filter { element ->
                val versionName = element.jsonObject["version"]!!.jsonPrimitive.content
                val libVersion = versionName.substringBeforeLast('.').toDouble()
                libVersion >= ExtensionLoader.LIB_VERSION_MIN && libVersion <= ExtensionLoader.LIB_VERSION_MAX
            }
            .map { element ->
                val name = element.jsonObject["name"]!!.jsonPrimitive.content.substringAfter("Tachiyomi: ")
                val pkgName = element.jsonObject["pkg"]!!.jsonPrimitive.content
                val apkName = element.jsonObject["apk"]!!.jsonPrimitive.content
                val versionName = element.jsonObject["version"]!!.jsonPrimitive.content
                val versionCode = element.jsonObject["code"]!!.jsonPrimitive.long
                val lang = element.jsonObject["lang"]!!.jsonPrimitive.content
                val nsfw = element.jsonObject["nsfw"]!!.jsonPrimitive.int == 1
                val sources = element.jsonObject["sources"]?.jsonArray?.map f@{
                    val sName = it.jsonObject["name"]?.jsonPrimitive?.content ?: return@f null
                    val sId = it.jsonObject["id"]?.jsonPrimitive?.content ?: return@f null
                    val sLang = it.jsonObject["lang"]?.jsonPrimitive?.content ?: ""
                    val sBaseUrl = it.jsonObject["baseUrl"]?.jsonPrimitive?.content ?: ""
                    Extension.AvailableSource(sName, sId, sLang, sBaseUrl)
                }?.filterNotNull()
                val icon = "${REPO_URL_PREFIX}icon/${apkName.replace(".apk", ".png")}"

                Extension.Available(name, pkgName, versionName, versionCode, lang, nsfw, apkName, icon, sources)
            }
    }

    fun getApkUrl(extension: ExtensionManager.ExtensionInfo): String {
        return "${REPO_URL_PREFIX}apk/${extension.apkName}"
    }
}

private const val REPO_URL_PREFIX = "https://raw.githubusercontent.com/tachiyomiorg/tachiyomi-extensions/repo/"
