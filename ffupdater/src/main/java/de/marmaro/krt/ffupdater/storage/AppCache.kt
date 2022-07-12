package de.marmaro.krt.ffupdater.storage

import android.content.Context
import android.os.Environment
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.LatestUpdate
import java.io.File

class AppCache(val app: App) {
    fun getFile(context: Context): File {
        return File(getCacheFolder(context), "${app.impl.packageName}.apk")
    }

    suspend fun isAvailable(context: Context, available: LatestUpdate?): Boolean {
        val file = getFile(context)
        if (available == null || !file.exists() || file.length() == 0L) {
            return false
        }
        return app.impl.isAvailableVersionEqualToArchive(context, file, available)
    }

    private fun getCacheFolder(context: Context): File {
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return checkNotNull(downloadFolder) { "The external 'Download' folder of the app should exists." }
    }

    fun delete(context: Context) {
        val file = getFile(context)
        if (file.exists()) {
            val success = file.delete()
            check(success) { "Fail to delete file '${file.absolutePath}'." }
        }
    }
}