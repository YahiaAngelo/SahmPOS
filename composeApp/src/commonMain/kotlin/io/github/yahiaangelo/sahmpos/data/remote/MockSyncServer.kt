package io.github.yahiaangelo.sahmpos.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.random.Random

class MockSyncServer(
    seed: Long = 1L,
    private val failureRate: Double = 0.15,
    private val conflictRate: Double = 0.10,
) {
    private val random = Random(seed)
    private val storage = mutableMapOf<String, OrderDto>()
    private val json = Json { ignoreUnknownKeys = true }

    private val _events = ArrayDeque<String>()
    val events: List<String> get() = _events.toList()

    private fun record(msg: String) {
        _events.addLast(msg)
        while (_events.size > 50) _events.removeFirst()
    }

    fun createClient(): HttpClient {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            val method = request.method.value
            when {
                method == "POST" && path == "/orders" -> handlePostOrder(request)
                method == "GET" && path == "/health" -> respond(
                    content = """{"ok":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
                else -> respond(
                    content = """{"error":"not found"}""",
                    status = HttpStatusCode.NotFound,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
    }

    private suspend fun MockRequestHandleScope.handlePostOrder(request: HttpRequestData) =
        run {
            val bytes = bodyBytes(request.body)
            val dto = try {
                json.decodeFromString(OrderDto.serializer(), bytes.decodeToString())
            } catch (e: Throwable) {
                record("Malformed payload: ${e.message}")
                return respond(
                    content = """{"error":"bad request"}""",
                    status = HttpStatusCode.BadRequest,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val roll = random.nextDouble()
            when {
                roll < failureRate -> {
                    record("503 Service Unavailable for ${dto.id}")
                    respond(
                        content = """{"error":"upstream busy"}""",
                        status = HttpStatusCode.ServiceUnavailable,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                roll < failureRate + conflictRate -> {
                    val serverVersion = (storage[dto.id]?.version ?: 0L) + 1
                    storage[dto.id] = dto.copy(version = serverVersion)
                    record("409 Conflict for ${dto.id}: server v$serverVersion")
                    val body = json.encodeToString(
                        ConflictResponse.serializer(),
                        ConflictResponse(dto.id, serverVersion, "version conflict")
                    )
                    respond(
                        content = body,
                        status = HttpStatusCode.Conflict,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> {
                    storage[dto.id] = dto
                    record("200 OK ${dto.id} v${dto.version}")
                    val body = json.encodeToString(OrderAck.serializer(), OrderAck(dto.id, dto.version))
                    respond(
                        content = body,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }
        }

    private suspend fun bodyBytes(content: OutgoingContent): ByteArray = when (content) {
        is TextContent -> content.bytes()
        is ByteArrayContent -> content.bytes()
        else -> ByteArray(0)
    }
}