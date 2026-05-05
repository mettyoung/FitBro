package com.mettyoung.fitbro.auth

object CronometerOAuthConfig {
    const val CLIENT_ID = "fitbro-app"
    const val REDIRECT_URI = "com.mettyoung.fitbro://oauth/callback"
    const val AUTH_URL = "https://cronometer.com/oauth2/authorize"
    const val TOKEN_URL = "https://cronometer.com/oauth2/token"
    const val SCOPE = "food activity"
}
