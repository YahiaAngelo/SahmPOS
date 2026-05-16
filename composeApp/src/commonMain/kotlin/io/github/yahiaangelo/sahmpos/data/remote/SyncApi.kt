package io.github.yahiaangelo.sahmpos.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

sealed interface PushResult {
    data class Accepted(val version: Long) : PushResult
    data class Conflict(val serverVersion: Long) : PushResult
    data class RetryableError(val code: Int, val message: String) : PushResult
    data class FatalError(val code: Int, val message: String) : PushResult
}

class SyncApi(private val client: HttpClient, private val baseUrl: String = "https://api.sahmpos.local") {

    suspend fun pushOrder(order: OrderDto): PushResult = try {
        val response: HttpResponse = client.post("$baseUrl/orders") {
            contentType(ContentType.Application.Json)
            setBody(order)
        }
        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> {
                val ack: OrderAck = response.body()
                PushResult.Accepted(ack.acceptedVersion)
            }
            HttpStatusCode.Conflict -> {
                val c: ConflictResponse = response.body()
                PushResult.Conflict(c.serverVersion)
            }
            HttpStatusCode.ServiceUnavailable,
            HttpStatusCode.GatewayTimeout,
            HttpStatusCode.RequestTimeout,
            HttpStatusCode.TooManyRequests,
            HttpStatusCode.InternalServerError -> PushResult.RetryableError(
                response.status.value, response.status.description
            )
            else -> PushResult.FatalError(response.status.value, response.status.description)
        }
    } catch (e: Throwable) {
        PushResult.RetryableError(0, e.message ?: "network error")
    }
}