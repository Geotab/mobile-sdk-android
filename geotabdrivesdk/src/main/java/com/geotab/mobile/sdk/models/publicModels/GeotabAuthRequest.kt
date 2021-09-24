package com.geotab.mobile.sdk.models.publicModels

import java.io.Serializable

data class GeotabAuthRequest(
    val method: String,
    val params: GeotabAuthParams
)

data class GeotabAuthParams(
    val database: String,
    val userName: String,
    val password: String
)

data class GeotabCredentials(
    val userName: String,
    val database: String,
    val sessionId: String
) : Serializable

data class AuthResponse(
    val result: CredentialResult?
)

data class CredentialResult(
    val credentials: GeotabCredentials,
    val path: String
) : Serializable
