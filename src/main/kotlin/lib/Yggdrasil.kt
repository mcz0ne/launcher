package lib

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class Yggdrasil(clientToken: String) {
    class ForbiddenException(message: String) : Exception(message)

    @Serializable
    data class AuthenticationAgent(
        val name: String = "minecraft",
        val version: Int = 1
    )

    @Serializable
    data class AuthenticationRequest(
        val agent: AuthenticationAgent = AuthenticationAgent(),
        val username: String,
        val password: String,
        val clientToken: String?,
        val requestUser: Boolean? = true
    )

    @Serializable
    data class AuthenticationSelectedProfile(
        val id: String,
        val name: String
    )

    @Serializable
    data class AuthenticationUser(
        val id: String,
        val username: String
    )

    @Serializable
    data class AuthenticationResponse(
        val accessToken: String,
        val clientToken: String,
        val selectedProfile: AuthenticationSelectedProfile?,
        val user: AuthenticationUser?
    )

    @Serializable
    data class ErrorResponse(
        val error: String,
        val errorMessage: String
    )

    companion object {
        private const val authServer = "https://authserver.mojang.com/"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private val json = Json(JsonConfiguration.Stable.copy(strictMode = false)) // we are deliberatly not defining all keys
        private val http = OkHttpClient()
    }

    private val theClientToken = clientToken

    private fun request(url: String, payload: String): Response {
        val req = Request.Builder()
            .url(authServer + url)
            .post(payload.toRequestBody(JSON))
            .header("Content-Type", "application/json")
            .build()

        val res = http.newCall(req).execute()

        when (res.code) {
            200 -> return res
            403 -> {
                val err = json.parse(ErrorResponse.serializer(), res.body!!.string())
                throw ForbiddenException(err.errorMessage)
            }
            // should not happen
            404 /* Not found */ -> throw Exception("Invalid endpoint")
            405 /* Method not allowed */ -> throw Exception("Not a post request")
            415 /* Unsupported media type */ -> throw Exception("Not a JSON body")
            else -> throw RuntimeException("Unexpected return code: ${res.code}")
        }
    }

    fun authenticate(username: String, password: String) {
        request(
            "authenticate", json.stringify(
                AuthenticationRequest.serializer(), AuthenticationRequest(
                    username = username,
                    password = password,
                    clientToken = theClientToken
                )
            )
        ).use {
            val res = json.parse(AuthenticationResponse.serializer(), it.body!!.string())
            println(res)
        }
    }
}