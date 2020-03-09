package lib.yggdrasil

import lib.Util
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class Yggdrasil(private val clientToken: String) {
    companion object {
        private val logger = KotlinLogging.logger {}
        private const val authServer = "https://authserver.mojang.com/"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private fun request(url: String, payload: String): Response {
        logger.debug("Sending request to {}", url)
        logger.trace("> {}", payload)
        val req = Request.Builder()
            .url(authServer + url)
            .post(payload.toRequestBody(JSON))
            .header("Content-Type", JSON.toString())
            .build()

        val res = Util.http.newCall(req).execute()
        logger.debug("Request to {} returned {}", url, res.code)

        when (res.code) {
            200, 204 -> return res
            400 /* Bad Request */ -> {
                val err = Util.json.parse(ErrorResponse.serializer(), res.body!!.string())
                throw BadRequestException(err.errorMessage)
            }
            403 /* Forbidden */ -> {
                val err = Util.json.parse(ErrorResponse.serializer(), res.body!!.string())
                throw ForbiddenException(err.errorMessage)
            }

            // should not happen
            404 /* Not found */ -> throw StatusCodeException("Invalid endpoint", 404)
            405 /* Method not allowed */ -> throw StatusCodeException("Not a post request", 405)
            415 /* Unsupported media type */ -> throw StatusCodeException("Not a JSON body", 415)
            else -> throw StatusCodeException(
                "Unexpected return code: ${res.code} ${res.message}",
                res.code
            )
        }
    }

    fun authenticate(username: String, password: String): Account {
        request(
            "authenticate", Util.json.stringify(
                AuthenticationRequest.serializer(),
                AuthenticationRequest(
                    username = username,
                    password = password,
                    clientToken = clientToken
                )
            )
        ).use {
            val res = Util.json.parse(AuthenticationResponse.serializer(), it.body!!.string())

            return Account(
                id = res.user!!.id,
                uuid = res.selectedProfile!!.id,
                email = res.user.username,
                username = res.selectedProfile.name,
                accessToken = res.accessToken
            )
        }
    }

    fun validate(acc: Account): Boolean {
        try {
            request(
                "validate", Util.json.stringify(
                    ValidateRequest.serializer(),
                    ValidateRequest(
                        accessToken = acc.accessToken,
                        clientToken = clientToken
                    )
                )
            ).use {
                return true
            }
        } catch (ex: ForbiddenException) {
            return false
        }
    }

    fun refresh(acc: Account): Account {
        request(
            "refresh", Util.json.stringify(
                RefreshRequest.serializer(),
                RefreshRequest(
                    accessToken = acc.accessToken,
                    clientToken = clientToken,
                    selectedProfile = AuthenticationSelectedProfile(
                        id = acc.uuid,
                        name = acc.username
                    )
                )
            )
        ).use {
            val res = Util.json.parse(AuthenticationResponse.serializer(), it.body!!.string())

            return Account(
                res.user!!.id,
                res.selectedProfile!!.id,
                res.user.username,
                res.selectedProfile.name,
                res.accessToken
            )
        }
    }

    fun invalidate(acc: Account) {
        try {
            request(
                "invalidate", Util.json.stringify(
                    ValidateRequest.serializer(),
                    ValidateRequest(
                        accessToken = acc.accessToken,
                        clientToken = clientToken
                    )
                )
            )
        } catch (ex: Exception) {
            // noop
        }
    }
}