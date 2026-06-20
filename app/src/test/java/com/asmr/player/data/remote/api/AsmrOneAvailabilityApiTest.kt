package com.asmr.player.data.remote.api

import com.asmr.player.data.remote.NetworkHeaders
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AsmrOneAvailabilityApiTest {
    @Test
    fun check_mapsMatchedRjsToRequestedWorkno() {
        val api = AsmrOneAvailabilityApi(
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    response(
                        chain,
                        200,
                        """{"items":[{"rj":"RJ000001","collected":true,"workId":1583603,"matchedRjs":["RJ01583603"]}]}"""
                    )
                }
                .build(),
            gson = Gson(),
            baseUrlProvider = { "https://eara.test" }
        )

        val result = kotlinx.coroutines.runBlocking { api.check(listOf("RJ01583603")) }

        assertEquals(true, result["RJ01583603"])
    }

    @Test
    fun findCollectedWorkId_returnsBackendWorkIdFromMatchedRj() {
        var requestedPath = ""
        var hasSilentHeader = false
        val api = AsmrOneAvailabilityApi(
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    requestedPath = chain.request().url.encodedPath
                    hasSilentHeader = chain.request().header(NetworkHeaders.HEADER_SILENT_IO_ERROR) == NetworkHeaders.SILENT_IO_ERROR_ON
                    response(
                        chain,
                        200,
                        """{"items":[{"rj":"RJ000001","collected":true,"workId":1583603,"matchedRjs":["RJ01583603"]}]}"""
                    )
                }
                .build(),
            gson = Gson(),
            baseUrlProvider = { "https://eara.test" }
        )

        val workId = kotlinx.coroutines.runBlocking { api.findCollectedWorkId("RJ01583603") }

        assertEquals("/api/asmr-one/availability", requestedPath)
        assertTrue(hasSilentHeader)
        assertEquals("1583603", workId)
    }

    @Test
    fun getTrackTree_returnsBackendTree() {
        var requestedPath = ""
        var requestedWorkId = ""
        var hasSilentHeader = false
        val api = AsmrOneAvailabilityApi(
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    requestedPath = chain.request().url.encodedPath
                    requestedWorkId = chain.request().url.queryParameter("workId").orEmpty()
                    hasSilentHeader = chain.request().header(NetworkHeaders.HEADER_SILENT_IO_ERROR) == NetworkHeaders.SILENT_IO_ERROR_ON
                    response(chain, 200, """[{"title":"01.mp3","mediaDownloadUrl":"https://example.test/01.mp3"}]""")
                }
                .build(),
            gson = Gson(),
            baseUrlProvider = { "https://eara.test" }
        )

        val tree = kotlinx.coroutines.runBlocking { api.getTrackTree("1590748") }

        assertEquals("/api/asmr-one/tracks", requestedPath)
        assertEquals("1590748", requestedWorkId)
        assertTrue(hasSilentHeader)
        assertEquals(1, tree.size)
        assertEquals("01.mp3", tree.first().title)
        assertEquals("https://example.test/01.mp3", tree.first().mediaDownloadUrl)
    }

    @Test(expected = IOException::class)
    fun getTrackTree_throwsOnBackendMiss() {
        val api = AsmrOneAvailabilityApi(
            okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain -> response(chain, 404, """{"error":"not_found"}""") }
                .build(),
            gson = Gson(),
            baseUrlProvider = { "https://eara.test" }
        )

        kotlinx.coroutines.runBlocking { api.getTrackTree("1590748") }
    }

    private fun response(chain: Interceptor.Chain, code: Int, body: String): Response {
        return Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
    }
}
