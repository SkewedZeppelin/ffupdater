package de.marmaro.krt.ffupdater.network.fdroid

import android.content.Context
import android.content.SharedPreferences
import com.github.ivanshafran.sharedpreferencesmock.SPMockBuilder
import de.marmaro.krt.ffupdater.BaseTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
class FdroidConsumerIT : BaseTest() {

    @MockK
    lateinit var context: Context

    lateinit var sharedPreferences: SharedPreferences

    @BeforeEach
    fun setUp() {
        sharedPreferences = SPMockBuilder().createSharedPreferences()
        every { context.packageName } returns "de.marmaro.krt.ffupdater"
        every { context.cacheDir } returns File(".")
        every { context.getSharedPreferences(any(), any()) } returns sharedPreferences
        sharedPreferences.edit().putBoolean("network__trust_user_cas", false)
    }

    companion object {
        private const val API_URL = "https://f-droid.org/api/v1/packages/us.spotco.fennec_dos"
    }

    //TODO
//    private fun prepareApiResponse() {
//        val apiResponse = """
//            {
//              "packageName": "us.spotco.fennec_dos",
//              "suggestedVersionCode": 21011120,
//              "packages": [
//                {
//                  "versionName": "101.1.1",
//                  "versionCode": 21011120
//                },
//                {
//                  "versionName": "101.1.1",
//                  "versionCode": 21011100
//                },
//                {
//                  "versionName": "100.3.0",
//                  "versionCode": 21003020
//                },
//                {
//                  "versionName": "100.3.0",
//                  "versionCode": 21003000
//                }
//              ]
//            }
//        """.trimIndent()
//        coEvery {
//            fileDownloader.downloadSmallFile(API_URL)
//        } returns Gson().fromJson(apiResponse, AppInfo::class.java)
//
//        val apiResponse2 = """
//            {
//                "last_commit_id":"5eb1934163f60fe64757dc81effa33255ecd5808"
//            }
//        """.trimIndent()
//        coEvery {
//            fileDownloader.downloadSmallFile("https://gitlab.com/api/v4/projects/36528/repository/files/metadata%2Fus.spotco.fennec_dos.yml?ref=master").use<ResponseBody, Nothing> {
//                //TODO
//                return Gson().fromJson(it.charStream().buffered(), GitlabRepositoryFilesMetadata::class
//                    .java)
//            }
//            Unit
//        } returns Gson().fromJson(apiResponse2, GitlabRepositoryFilesMetadata::class.java)
//
//        val apiResponse3 = """
//            {
//                "created_at":"2022-10-11T08:02:58.000+00:00"
//            }
//        """.trimIndent()
//        coEvery {
//            apiConsumer.consume(
//                "https://gitlab.com/api/v4/projects/36528/repository/commits/5eb1934163f60fe64757dc81effa33255ecd5808",
//                fileDownloader,
//                GitlabRepositoryCommits::class
//            )
//        } returns Gson().fromJson(apiResponse3, GitlabRepositoryCommits::class.java)
//    }
//
//    @Test
//    fun `get first version code and download url from api call`() {
//        val fdroidConsumer = FdroidConsumer(apiConsumer)
//        val result = runBlocking {
//            fdroidConsumer.getLatestUpdate("us.spotco.fennec_dos", fileDownloader, 1)
//        }
//        assertEquals("101.1.1", result.versionName)
//        assertEquals(21011100, result.versionCode)
//        assertEquals("https://f-droid.org/repo/us.spotco.fennec_dos_21011100.apk", result.downloadUrl)
//    }
//
//    @Test
//    fun `get second version code and download url from api call`() {
//        val fdroidConsumer = FdroidConsumer(apiConsumer)
//        val result = runBlocking {
//            fdroidConsumer.getLatestUpdate("us.spotco.fennec_dos", fileDownloader, 2)
//        }
//        assertEquals("101.1.1", result.versionName)
//        assertEquals(21011120, result.versionCode)
//        assertEquals("https://f-droid.org/repo/us.spotco.fennec_dos_21011120.apk", result.downloadUrl)
//    }
}