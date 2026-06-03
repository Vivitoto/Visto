package app.visto.data.webdav

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WebDavAuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var interceptor: WebDavAuthInterceptor
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        interceptor = WebDavAuthInterceptor()
        client = OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun addsBasicAuthForMatchingHostAndPathPrefix() {
        interceptor.setAccount(server.url("/dav").toString(), "alice", "secret")
        server.enqueue(MockResponse().setResponseCode(200))
        client.newCall(Request.Builder().url(server.url("/dav/Photos/a.jpg")).build())
            .execute().use { it.body?.string() }
        val recorded = server.takeRequest()
        assertEquals("Basic YWxpY2U6c2VjcmV0", recorded.getHeader("Authorization"))
    }

    @Test
    fun leavesRequestsToOtherPathsUntouched() {
        interceptor.setAccount(server.url("/dav").toString(), "alice", "secret")
        server.enqueue(MockResponse().setResponseCode(200))
        client.newCall(Request.Builder().url(server.url("/public/logo.png")).build())
            .execute().use { it.body?.string() }
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun isNoOpWhenNoAccountConfigured() {
        server.enqueue(MockResponse().setResponseCode(200))
        client.newCall(Request.Builder().url(server.url("/dav/anything")).build())
            .execute().use { it.body?.string() }
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }
}
