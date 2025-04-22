package com.promikhail.research.ip4list.tools.shardedArray

interface IShardedArray {

    suspend fun incBlockValue(value: UInt)

    fun analyze(): Triple<String, Long, Long>

}

