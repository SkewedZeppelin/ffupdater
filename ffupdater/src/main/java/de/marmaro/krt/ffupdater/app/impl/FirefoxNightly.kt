package de.marmaro.krt.ffupdater.app.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.MainThread
import androidx.preference.PreferenceManager
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.AppUpdateStatus
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import de.marmaro.krt.ffupdater.device.ABI
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.device.DeviceSdkTester
import de.marmaro.krt.ffupdater.network.FileDownloader
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.mozillaci.MozillaCiJsonConsumer
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v3.firefox-android.apks.fenix-nightly.latest
 * https://www.apkmirror.com/apk/mozilla/firefox-fenix/
 * https://firefox-ci-tc.services.mozilla.com/tasks/CeuwnI5oTuSiZPrCjjc8kA
 */
class FirefoxNightly(
    private val consumer: MozillaCiJsonConsumer = MozillaCiJsonConsumer.INSTANCE,
    private val deviceAbiExtractor: DeviceAbiExtractor = DeviceAbiExtractor.INSTANCE,
    private val deviceSdkTester: DeviceSdkTester = DeviceSdkTester.INSTANCE,
) : AppBase() {
    override val app = App.FIREFOX_NIGHTLY
    override val packageName = "org.mozilla.fenix"
    override val title = R.string.firefox_nightly__title
    override val description = R.string.firefox_nightly__description
    override val installationWarning = R.string.generic_app_warning__beta_version
    override val downloadSource = "Mozilla CI"
    override val icon = R.drawable.ic_logo_firefox_nightly
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "5004779088e7f988d5bc5cc5f8798febf4f8cd084a1b2a46efd4c8ee4aeaf211"
    override val supportedAbis = ARM32_ARM64_X86_X64
    override val projectPage =
        "https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v3.firefox-android.apks.fenix-nightly.latest"
    override val displayCategory = DisplayCategory.FROM_MOZILLA

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun findLatestUpdate(
        context: Context,
        fileDownloader: FileDownloader,
    ): LatestUpdate? {
        Log.d(LOG_TAG, "check for latest version")
        val deviceSettings = DeviceSettingsHelper(context)

        val abiString = when (deviceAbiExtractor.findBestAbi(supportedAbis, deviceSettings.prefer32BitApks)) {
            ABI.ARMEABI_V7A -> "armeabi-v7a"
            ABI.ARM64_V8A -> "arm64-v8a"
            ABI.X86 -> "x86"
            ABI.X86_64 -> "x86_64"
            else -> throw IllegalArgumentException("ABI is not supported")
        }
        // clicking on public/logs/chain_of_trust.log in
        // https://firefox-ci-tc.services.mozilla.com/tasks/index/mobile.v3.firefox-android.apks.fenix-nightly.latest/arm64-v8a
        // will lead you to
        // https://firefoxci.taskcluster-artifacts.net/CeuwnI5oTuSiZPrCjjc8kA/0/public/logs/chain_of_trust.log
        val result = consumer.updateCheck(
            taskId = "CeuwnI5oTuSiZPrCjjc8kA",
            abiString = abiString,
            fileDownloader = fileDownloader,
        )
        val downloadUrl = "https://firefox-ci-tc.services.mozilla.com/api/index/v1/task/" +
                "mobile.v3.firefox-android.apks.fenix-nightly.latest.${abiString}/artifacts/" +
                "public%2Fbuild%2Ffenix%2F${abiString}%2Ftarget.apk"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val releaseDate = ZonedDateTime.parse(result.releaseDate, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        val version = formatter.format(releaseDate)
        Log.i(LOG_TAG, "found latest version $version")
        return LatestUpdate(
            downloadUrl = downloadUrl,
            version = version,
            publishDate = result.releaseDate,
            exactFileSizeBytesOfDownload = null,
            fileHash = result.fileHash,
        )
    }

    override fun isAvailableVersionHigherThanInstalled(
        context: Context,
        available: LatestUpdate,
    ): Boolean {
        return try {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val installedSha256Hash = preferences.getString(INSTALLED_SHA256_HASH, "unknown")
            val installedVersionCode = preferences.getLong(INSTALLED_VERSION_CODE, -1)
            val sameHex = available.fileHash?.hexValue == installedSha256Hash
            val sameVersionCode = getVersionCode(context) == installedVersionCode
            !(sameHex && sameVersionCode)
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @SuppressLint("ApplySharedPref")
    override fun appIsInstalledCallback(context: Context, available: AppUpdateStatus) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putLong(INSTALLED_VERSION_CODE, getVersionCode(context))
            .putString(INSTALLED_SHA256_HASH, available.latestUpdate.fileHash?.hexValue)
            .commit()
        // this must be called last because the update is only recognized after setting the other values
        super.appIsInstalledCallback(context, available)
    }

    /**
     * @throws PackageManager.NameNotFoundException
     */
    @Suppress("DEPRECATION")
    private fun getVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
        if (deviceSdkTester.supportsAndroid9()) {
            return packageInfo.longVersionCode
        }
        return packageInfo.versionCode.toLong()
    }

    companion object {
        private const val LOG_TAG = "FirefoxNightly"
        const val INSTALLED_VERSION_CODE = "firefox_nightly_installed_version_code"
        const val INSTALLED_SHA256_HASH = "firefox_nightly_installed_sha256_hash"
    }
}