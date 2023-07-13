package de.marmaro.krt.ffupdater.app.impl

import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.MainThread
import de.marmaro.krt.ffupdater.R
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.app.entity.LatestVersion
import de.marmaro.krt.ffupdater.device.DeviceAbiExtractor
import de.marmaro.krt.ffupdater.network.exceptions.NetworkException
import de.marmaro.krt.ffupdater.network.fdroid.CustomRepositoryConsumer
import de.marmaro.krt.ffupdater.network.file.CacheBehaviour
import de.marmaro.krt.ffupdater.settings.DeviceSettingsHelper

/**
 * https://f-droid.org/en/packages/us.spotco.fennec_dos/
 */
@Keep
object MullFromRepo : AppBase() {
    override val app = App.MULL_FROM_REPO
    override val packageName = "us.spotco.fennec_dos"
    override val title = R.string.mull__title
    override val description = R.string.mull__description
    override val downloadSource = "https://divestos.org/fdroid/official"
    override val icon = R.drawable.ic_logo_mull
    override val minApiLevel = Build.VERSION_CODES.LOLLIPOP
    override val supportedAbis = ARM32_ARM64

    @Suppress("SpellCheckingInspection")
    override val signatureHash = "260e0a49678c78b70c02d6537add3b6dc0a17171bbde8ce75fd4026a8a3e18d2"
    override val projectPage = "https://divestos.org/fdroid/official/"
    override val displayCategory = DisplayCategory.BASED_ON_FIREFOX

    @MainThread
    @Throws(NetworkException::class)
    override suspend fun fetchLatestUpdate(context: Context, cacheBehaviour: CacheBehaviour): LatestVersion {
        val abi = DeviceAbiExtractor.findBestAbi(supportedAbis, DeviceSettingsHelper.prefer32BitApks)
        return CustomRepositoryConsumer.getLatestUpdate(
            "https://divestos.org/fdroid/official",
            packageName,
            abi,
            cacheBehaviour
        )
    }
}