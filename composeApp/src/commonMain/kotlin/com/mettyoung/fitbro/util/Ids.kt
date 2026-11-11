package com.mettyoung.fitbro.util

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newUuid(): String = Uuid.random().toString()
