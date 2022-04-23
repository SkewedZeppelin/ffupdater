package de.marmaro.krt.ffupdater.download

import android.content.Context
import android.os.Environment
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.AvailableVersionResult
import java.io.File

class AppCache(val app: App) {
    fun getFile(context: Context): File {
        return File(getCacheFolder(context), "${app.detail.packageName}.apk")
    }

    suspend fun isAvailable(context: Context, available: AvailableVersionResult): Boolean {
        val file = getFile(context)
        if (file.exists()) {
            return app.detail.isAvailableVersionEqualToArchive(context, file, available)
        }
        return false
    }

    private fun getCacheFolder(context: Context): File {
        val downloadFolder = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return checkNotNull(downloadFolder) { "External download folder should exists." }
    }

    fun delete(context: Context) {
        val file = getFile(context)
        val success = file.delete()
        check(success) { "Fail to delete file '${file.absolutePath}'." }
    }
}