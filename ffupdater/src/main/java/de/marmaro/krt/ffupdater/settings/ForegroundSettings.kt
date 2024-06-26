package de.marmaro.krt.ffupdater.settings

import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.appcompat.app.AppCompatDelegate
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.device.DeviceSdkTester

@Keep
object ForegroundSettings {
    private lateinit var preferences: SharedPreferences

    /**
     * This function must be called from Application.onCreate() or this singleton can't be used
     */
    fun init(sharedPreferences: SharedPreferences) {
        preferences = sharedPreferences
    }

    val isUpdateCheckOnMeteredAllowed
        get() = preferences.getBoolean("foreground__update_check__metered", true)

    val isDownloadOnMeteredAllowed
        get() = preferences.getBoolean("foreground__download__metered", true)

    private val validAndroidThemes = listOf(
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_NO
    )

    val themePreference: Int
        get() {
            val theme = preferences.getString("foreground__theme_preference", null)?.toIntOrNull()
            return when {
                theme in validAndroidThemes -> theme!!
                // return default values because theme is invalid and could be null
                DeviceSdkTester.supportsAndroid10Q29() -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                else -> AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
            }
        }

    val isDeleteUpdateIfInstallSuccessful
        get() = preferences.getBoolean("foreground__delete_cache_if_install_successful", true)

    val isDeleteUpdateIfInstallFailed
        get() = preferences.getBoolean("foreground__delete_cache_if_install_failed", true)

    val isHideAppsSignedByDifferentCertificate
        get() = preferences.getBoolean("foreground__hide_apps_signed_by_different_certificate", false)

    var hiddenApps: List<App>
        get() {
            return (preferences.getStringSet("foreground__hidden_apps", null) ?: setOf())
                .mapNotNull {
                    try {
                        App.valueOf(it)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
        }
        set(value) {
            val appNames = value.map { it.name }.toSet()
            preferences.edit()
                .putStringSet("foreground__hidden_apps", appNames)
                .apply()
        }

    fun hideApp(app: App) {
        val apps = hiddenApps.toMutableList()
        apps.add(app)
        hiddenApps = apps
    }

}