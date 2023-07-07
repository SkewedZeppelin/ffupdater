package de.marmaro.krt.ffupdater.app.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.network.file.FileDownloader
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android/
 * https://storage.googleapis.com/chromium-browser-snapshots/index.html?prefix=Android_Arm64/
 */
@Keep
class Chromium : AppBase() {
    override val app = App.CHROMIUM
    override val packageName = "org.chromium.chrome"
    override val title = R.string.chromium__title
    override val description = R.string.chromium__description
    override val installationWarning = R.string.chromium__warning
    override val downloadSource = "https://storage.googleapis.com/chromium-browser-snapshots"
    override val icon = R.drawable.ic_logo_chromium
    override val minApiLevel = Build.VERSION_CODES.M
    override val supportedAbis = ARM32_ARM64
    override val signatureHash = "32a2fc74d731105859e5a85df16d95f102d85b22099b8064c5d8915c61dad1e0"
    override val projectPage = "https://www.chromium.org/chromium-projects/"
    override val displayCategory = DisplayCategory.BETTER_THAN_GOOGLE_CHROME
    override val fileNameInZipArchive = "chrome-android/apks/ChromePublic.apk"

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestUpdate {
        val platform = findPlatform()
        val revision = findLatestRevision(platform, cacheBehaviour)
        val storageObject = findStorageObject(revision, platform, cacheBehaviour)
        return LatestUpdate(
            downloadUrl = storageObject.downloadUrl,
            version = revision,
            publishDate = storageObject.timestamp,
            exactFileSizeBytesOfDownload = storageObject.fileSizeBytes,
            fileHash = null,
        )
    }

    private fun findPlatform(): String {
        val platform = when (DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)) {
            ABI.ARM64_V8A -> "Android_Arm64"
            ABI.ARMEABI_V7A -> "Android"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        return platform
    }

    private suspend fun findLatestRevision(
        platform: String,
        cacheBehaviour: CacheBehaviour,
    ): String {
        return try {
            val slash = "%2F"
            val url = "$BASE_DOWNLOAD_URL/${platform}${slash}LAST_CHANGE?alt=media"
            FileDownloader.downloadStringWithCache(url, cacheBehaviour)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }
    }

    private suspend fun findStorageObject(
        revision: String,
        platform: String,
        cacheBehaviour: CacheBehaviour,
    ): StorageObject {
        val url = "$BASE_API_URL?delimiter=/&prefix=$platform/${revision}/chrome-android&$ALL_FIELDS"
        val storageObjects = try {
            FileDownloader.downloadJsonObjectWithCache(url, cacheBehaviour)
        } catch (e: NetworkException) {
            throw NetworkException("Fail to request the latest Vivaldi version.", e)
        }

        val items = storageObjects["items"].asJsonArray
        checkNotNull(items)
        check(items.size() == 1)
        val storageObject = items[0].asJsonObject
        val storageObjectKotlin = StorageObject(
            kind = storageObject["kind"].asString,
            downloadUrl = storageObject["mediaLink"].asString,
            name = storageObject["name"].asString,
            fileSizeBytes = storageObject["size"].asLong,
            timestamp = storageObject["updated"].asString,
        )

        check(storageObjectKotlin.kind == "storage#object")
        check(storageObjectKotlin.name == "$platform/$revision/chrome-android.zip")
        return storageObjectKotlin
    }

    @SuppressLint("ApplySharedPref")
    override fun appIsInstalledCallback(context: Context, available: AppUpdateStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(INSTALLED_VERSION_REVISION, available.latestUpdate.version)
            .putString(INSTALLED_VERSION_TIMESTAMP, available.latestUpdate.publishDate)
            .commit()
        // this must be called last because the update is only recognized after setting the other values
        super.appIsInstalledCallback(context, available)
    }

    override fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: LatestUpdate,
    ): Boolean {
        try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (preferences.getString(INSTALLED_VERSION_REVISION, "-1") != available.version) {
                return true
            }
            if (preferences.getString(INSTALLED_VERSION_TIMESTAMP, "") != available.publishDate) {
                return true
            }
            return false
        } catch (e: PackageManager.NameNotFoundException) {
            return false
        }
    }

    @Keep
    private data class StorageObject(
        val kind: String,
        val downloadUrl: String,
        val name: String,
        val fileSizeBytes: Long,
        val timestamp: String,
    )

    companion object {
        private const val LOG_TAG = "Chromium"
        const val INSTALLED_VERSION_REVISION = "chromium__installed_version_revision"
        const val INSTALLED_VERSION_TIMESTAMP = "chromium__installed_version_timestamp"

        const val BASE_API_URL = "https://www.googleapis.com/storage/v1/b/chromium-browser-snapshots/o"
        const val BASE_DOWNLOAD_URL =
            "https://www.googleapis.com/download/storage/v1/b/chromium-browser-snapshots/o"
        const val ALL_FIELDS =
            "fields=items(kind,mediaLink,metadata,name,size,updated),kind,prefixes,nextPageToken"
    }
}