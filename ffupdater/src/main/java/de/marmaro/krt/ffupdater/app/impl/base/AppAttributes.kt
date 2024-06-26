package de.marmaro.krt.ffupdater.app.impl.base

import androidx.annotation.Keep
import de.marmaro.krt.ffupdater.app.App
import de.marmaro.krt.ffupdater.app.entity.DisplayCategory
import de.marmaro.krt.ffupdater.device.ABI

@Keep
interface AppAttributes {
    val app: App
    val packageName: String
    val title: Int
    val description: Int
    val installationWarning: Int?
    val downloadSource: String
    val icon: Int
    val minApiLevel: Int
    val supportedAbis: List<ABI>
    val signatureHash: String
    val installableByUser: Boolean
    val projectPage: String
    val eolReason: Int?
    val displayCategory: List<DisplayCategory>
    val fileNameInZipArchive: String?
    val differentSignatureMessage: Int

    fun isEol() = (eolReason != null)
}