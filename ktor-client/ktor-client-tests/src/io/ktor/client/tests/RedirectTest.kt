package io.ktor.client.tests

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.jvm.javaio.*
import kotlin.test.*


open class RedirectTest(private val factory: HttpClientEngineFactory<*>) : TestWithKtor() {
    override val server: ApplicationEngine = embeddedServer(Jetty, serverPort) {
        routing {
            get("/") {
                call.respondRedirect("/get")
            }
            get("/get") {
                call.respondText("OK")
            }
            get("/infinity") {
                call.respondRedirect("/infinity")
            }
        }
    }

    @Test
    fun redirectTest(): Unit = runBlocking {
        HttpClient(factory) {
            install(HttpRedirect)
        }.use { client ->
            client.get<HttpResponse>(port = serverPort).use {
                assertEquals(HttpStatusCode.OK, it.status)
                assertEquals("OK", it.readText())
            }
        }
    }

    @Test
    fun infinityRedirect(): Unit {
        HttpClient(factory) {
            install(HttpRedirect)
        }.use { client ->
            assertFails {
                runBlocking {
                    client.get<HttpResponse>(port = serverPort, path = "/infinity")
                }
            }
        }
    }
}