package com.promikhail.research.ip4list.tools.shardedArray

import com.promikhail.research.ip4list.tools.formatWithSeparator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.measureTime


/**
 * Тестовый шардированный массив. В качестве блока выступает байт, поэтому в памяти занимает больше места, более 4ГБ.
 * Применяется для сверки результатов с [ShardedTwoBitArray].
 */
class ShardedByteArray : IShardedArray {

    companion object {
        private val BYTE_ZERO: Byte = 0
        private val ARRAY_SHARD_SIZE_INT: Int = 1024 * 1024
        private val ARRAY_SHARD_SIZE_UINT: UInt = ARRAY_SHARD_SIZE_INT.toUInt()
    }

    private val mtx = Mutex()

    private val byteArraysMap = HashMap<Int, Pair<Mutex, ByteArray>>(4)

    private suspend fun getArray(id: Int): Pair<Mutex, ByteArray> {
        mtx.withLock {
            return byteArraysMap.getOrPut(id) {
                Mutex() to ByteArray(ARRAY_SHARD_SIZE_INT)
            }
        }
    }

    @Suppress("ReplaceGetOrSet")
    override suspend fun incBlockValue(value: UInt) {
        val div = value.div(ARRAY_SHARD_SIZE_UINT)
        val rem = value.rem(ARRAY_SHARD_SIZE_UINT)
        val shard = div + if (rem > 0u) 1u else 0u
        val index = (value - ((shard - 1u) * ARRAY_SHARD_SIZE_UINT)).toInt() - 1

        val (mutex, arr) = getArray(shard.toInt())
        mutex.withLock {
            arr.set(index, arr.get(index).inc())
        }
    }

    override fun analyze(): Triple<String, Long, Long> {
        val sb = StringBuilder()
        sb.append("Analysis results:\n")

        val hm = HashMap<Byte, Long>(16)

        val t = measureTime {
            byteArraysMap.forEach { (_, ba) ->
                ba.second.forEach { b ->
                    if (b != BYTE_ZERO) {
                        hm[b] = (hm[b] ?: 0).inc()
                    }
                }
            }

            hm.toSortedMap().forEach { (b, i) ->
                sb.append("${i.formatWithSeparator().padStart(16)} addresses occur ${b.toString().padStart(8)} times\n")
            }
        }
        sb.append("Analysis took: ${t.inWholeSeconds.toString().padStart(8)} s")

        println(sb.toString())

        return Triple(sb.toString(), (hm[1] ?: 0L), t.inWholeSeconds)
    }
}

