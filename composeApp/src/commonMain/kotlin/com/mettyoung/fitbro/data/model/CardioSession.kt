package com.mettyoung.fitbro.data.model

data class CardioSession(
    val id: Long = 0,
    val date: String,
    val minutes: Int,
    val note: String? = null
)
