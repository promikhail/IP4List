package com.promikhail.research.ip4list.generator


const val MAX_IP_STR_SIZE_IN_BYTES = 17

interface IGenerator {
    fun generate(ipCount: Long, isCrLf: Boolean)
}