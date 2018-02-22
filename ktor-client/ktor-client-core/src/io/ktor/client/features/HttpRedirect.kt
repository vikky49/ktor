package io.ktor.client.features

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*

class HttpRedirect(
        val maxJumps: Int
) {

    class Config {
        var maxJumps: Int = Int.MAX_VALUE
    }

    companion object Feature : HttpClientFeature<Config, HttpRedirect> {
        override val key: AttributeKey<HttpRedirect> = AttributeKey("HttpRedirect")

        override suspend fun prepare(block: Config.() -> Unit): HttpRedirect = HttpRedirect(Config().apply(block).maxJumps)

        override fun install(feature: HttpRedirect, scope: HttpClient) {
            scope.sendPipeline.intercept scope@{ execute: suspend (HttpRequestData) -> HttpClientCall, origin: HttpRequestData ->
                var request = origin
                repeat(feature.maxJumps) {
                    val call = execute(request)
                    if (!call.response.status.isRedirect()) return@scope call
                    val location = call.response.headers[HttpHeaders.Location] ?: return@scope call

                    val oldUrl = request.url
                    request = HttpRequestBuilder().apply {
                        takeFrom(origin)
                        url.takeFrom(location)
                    }.build()

                    if (oldUrl == request.url) error("Redirect loop: $oldUrl")
                }

                error("Fail to redirect")
            }
        }
    }
}

private fun HttpStatusCode.isRedirect(): Boolean = when (this) {
    HttpStatusCode.MovedPermanently,
    HttpStatusCode.Found,
    HttpStatusCode.TemporaryRedirect,
    HttpStatusCode.PermanentRedirect -> true
    else -> false
}
