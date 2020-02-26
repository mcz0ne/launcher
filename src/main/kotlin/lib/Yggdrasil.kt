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
    data class AuthenticationAvailableProfiles(
        val agent: String,
        val id: String,
        val name: String,
        val userId: String,
        val createdAt: Int,
        val legacyProfile: Boolean,
        val suspended: Boolean,
        val paid: Boolean,
        val migrated: Boolean,
        val legacy: Boolean
    )

    @Serializable
    data class AuthenticationSelectedProfile(
        val id: String,
        val name: String,
        val userId: String,
        val createdAt: Int,
        val legacyProfile: Boolean,
        val suspended: Boolean,
        val paid: Boolean,
        val migrated: Boolean,
        val legacy: Boolean
    )

    @Serializable
    data class AuthenticationUserProperties(
        val name: String,
        val value: String
    )

    @Serializable
    data class AuthenticationUser(
        val id: String,
        val email: String,
        val username: String,
        val registerIp: String,
        val migratedFrom: String,
        val migratedAt: Int,
        val registeredAt: Int,
        val passwordChangedAt: Int,
        val dateOfBirth: Int,
        val suspended: Boolean,
        val blocked: Boolean,
        val secured: Boolean,
        val migrated: Boolean,
        val emailVerified: Boolean,
        val legacyUser: Boolean,
        val verifiedByParent: Boolean,
        val properties: List<AuthenticationUserProperties>
    )

    @Serializable
    data class AuthenticationResponse(
        val accessToken: String,
        val clientToken: String,
        val availableProfiles: List<AuthenticationAvailableProfiles>?,
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
        private val json = Json(JsonConfiguration.Stable)
        private val http = OkHttpClient()
    }

    private val theClientToken = clientToken

    private fun request(url: String, payload: String): Response {
        val req = Request.Builder()
            .url(authServer + url)
            .post(payload.toRequestBody(JSON))
            .header("Content-Type", "application/json")
            .build()

        return http.newCall(req).execute()
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
            val rawBody = it.body!!.string()

            val body = when (it.code) {
                200 -> it.body!!
                403 -> {
                    val err = json.parse(ErrorResponse.serializer(), rawBody)
                    throw ForbiddenException(err.errorMessage)
                }
                // should not happen
                404 /* Not found */ -> throw Exception("Invalid endpoint")
                405 /* Method not allowed */ -> throw Exception("Not a post request")
                415 /* Unsupported media type */ -> throw Exception("Not a JSON body")
                else -> throw RuntimeException("Unexpected return code: ${it.code}")
            }

            println(body)
        }
    }
}