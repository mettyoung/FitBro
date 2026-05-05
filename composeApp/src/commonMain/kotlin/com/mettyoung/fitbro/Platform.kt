package com.mettyoung.fitbro

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform