package com.mettyoung.fitbro.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CronometerOAuthRepository(
    private val tokenStorage: TokenStorage = createTokenStorage()
) : OAuthRepository {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    override fun buildAuthUrl(state: String): Pair<String, PkceChallenge> {
        val pkce = generatePkce()
        val url = buildString {
            append(CronometerOAuthConfig.AUTH_URL)
            append("?response_type=code")
            append("&client_id=${CronometerOAuthConfig.CLIENT_ID}")
            append("&redirect_uri=${CronometerOAuthConfig.REDIRECT_URI}")
            append("&scope=${CronometerOAuthConfig.SCOPE.replace(" ", "+")}")
            append("&state=$state")
            append("&code_challenge=${pkce.codeChallenge}")
            append("&code_challenge_method=S256")
        }
        return url to pkce
    }

    override suspend fun exchangeCode(code: String, codeVerifier: String): OAuthResult<OAuthToken> {
        return try {
            val response = httpClient.submitForm(
                url = CronometerOAuthConfig.TOKEN_URL,
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", CronometerOAuthConfig.REDIRECT_URI)
                    append("client_id", CronometerOAuthConfig.CLIENT_ID)
                    append("code_verifier", codeVerifier)
                }
            )
            if (!response.status.isSuccess()) {
                return OAuthResult.Failure(
                    OAuthError.HttpError(response.status.value, response.status.description)
                )
            }
            val tokenResponse = response.body<TokenResponse>()
            val token = tokenResponse.toOAuthToken()
            tokenStorage.saveToken(token)
            OAuthResult.Success(token)
        } catch (e: Exception) {
            OAuthResult.Failure(OAuthError.NetworkError(e))
        }
    }

    override suspend fun refreshToken(): OAuthResult<OAuthToken> {
        val stored = tokenStorage.loadToken()
            ?: return OAuthResult.Failure(OAuthError.NotAuthenticated)

        return try {
            val response = httpClient.submitForm(
                url = CronometerOAuthConfig.TOKEN_URL,
                formParameters = Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", stored.refreshToken)
                    append("client_id", CronometerOAuthConfig.CLIENT_ID)
                }
            )
            if (!response.status.isSuccess()) {
                return OAuthResult.Failure(
                    OAuthError.HttpError(response.status.value, response.status.description)
                )
            }
            val tokenResponse = response.body<TokenResponse>()
            val newToken = tokenResponse.toOAuthToken()
            tokenStorage.saveToken(newToken)
            OAuthResult.Success(newToken)
        } catch (e: Exception) {
            OAuthResult.Failure(OAuthError.NetworkError(e))
        }
    }

    override suspend fun getValidToken(): OAuthResult<OAuthToken> {
        val stored = tokenStorage.loadToken()
            ?: return OAuthResult.Failure(OAuthError.NotAuthenticated)

        return if (stored.isExpired(currentTimeMs())) {
            refreshToken()
        } else {
            OAuthResult.Success(stored)
        }
    }

    override fun isAuthenticated(): Boolean = tokenStorage.loadToken() != null

    override fun signOut() = tokenStorage.clearToken()
}

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String = "Bearer"
) {
    fun toOAuthToken(): OAuthToken = OAuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAtMs = currentTimeMs() + expiresIn * 1000L,
        tokenType = tokenType
    )
}
