package com.promikhail.research.ip4list.tools.shardedArray

import com.promikhail.research.ip4list.tools.formatWithSeparator
import com.promikhail.research.ip4list.tools.getItemsValuesFromByte
import com.promikhail.research.ip4list.tools.incrementBlock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.measureTime


/**
* Шардированный массив для хранения 2^32 блоков по 2 бита каждый, для экономии памяти.
* Массив должен занимать в памяти 1ГБ + небольшой размер вспомогательных структур.
*
* Шардирование, т.е. разделение массива на части, необходимо для ускорения, во избежание ожиданий
* при циклах блокировки/разблокировки частей при параллельном доступе нескольких потоков к частям массива.
*
* В значении блока храним значения от 0 до 3, так как для определения уникальности IP адреса этого достаточно.
*
* Блоки хранятся в массиве байтов. В одном байте содержится 4 блока (2 бита * 4 = 8 бит = 1 байт)
* Переданное UInt значение, как числовое представление IP адреса, используется как индекс в этом массиве,
* и указывает на блок.
* */
class ShardedTwoBitArray : IShardedArray {

    companion object {
        private const val ZERO_BYTE: Byte = 0

        // 2 bit per item
        private const val BLOCK_SIZE: Int = 2

        // 1 byte, 8 bit in an element
        private const val ELEMENT_SIZE: Int = 8

        private const val ELEMENTS_COUNT_PER_SHARD: Int = 64 * 1024 * 1024

        // 8 bit / 2 bit = 4 items in an element
        private val BLOCKS_COUNT_PER_ELEMENT: UInt = ELEMENT_SIZE.toUInt() / BLOCK_SIZE.toUInt()

        private val BLOCKS_COUNT_PER_SHARD: UInt = ELEMENTS_COUNT_PER_SHARD.toUInt() * BLOCKS_COUNT_PER_ELEMENT
    }

    private val mtx = Mutex()

    private val byteArraysMap = HashMap<Int, Pair<Mutex, ByteArray>>(32)

    private suspend fun getArray(id: Int): Pair<Mutex, ByteArray> {
        mtx.withLock {
            return byteArraysMap.getOrPut(id) {
                val array = ByteArray(ELEMENTS_COUNT_PER_SHARD) { ZERO_BYTE }
                Mutex() to array
            }
        }
    }

    // 1-based
    private fun getShardNumber(value: UInt): Int {
        val div = value.div(BLOCKS_COUNT_PER_SHARD)
        val rem = value.rem(BLOCKS_COUNT_PER_SHARD)
        val shard = (div + if (rem > 0u) 1u else 0u).toInt()
        return shard
    }

    private fun getElementAndBlockNumber(shardNumber: Int, value: UInt): Pair<Int, Int> {
        val offset = (shardNumber.toUInt() * BLOCKS_COUNT_PER_SHARD) - value + 1u
        val div = offset.div(BLOCKS_COUNT_PER_ELEMENT).toInt()
        val rem = offset.rem(BLOCKS_COUNT_PER_ELEMENT).toInt()
        val elementNumber = (div + if (rem > 0) 1 else 0) - 1
        val blockNumber = rem + 1
        return elementNumber to blockNumber
    }

    /**
     * Метод инкрементирует значение блока в массиве. Производится с блокировкой части массива в корутине.
     */
    @Suppress("ReplaceGetOrSet")
    override suspend fun incBlockValue(value: UInt) {
        val shardNumber = getShardNumber(value)
        val (elementNumber, blockNumber) = getElementAndBlockNumber(shardNumber, value)
        val (mutex, arr) = getArray(shardNumber)

        mutex.withLock {
            val b1 = arr.get(elementNumber)
            val b2 = incrementBlock(b1, blockNumber)
            arr.set(elementNumber, b2)
        }
    }

    /**
     * Анализирует массив. По сути производит группировку значений блоков с агрегированием их количества.
     * Возвращает строку с результатами и затраченное время.
     */
    override fun analyze(): Triple<String, Long, Long> {
        val sb = StringBuilder()
        sb.append("Analysis results:\n")

        val hm = HashMap<Byte, Long>(16)

        val t = measureTime {
            byteArraysMap.forEach { (_, ba) ->
                ba.second.forEach { i ->
                    val bb = getItemsValuesFromByte(i)
                    hm[bb[0]] = (hm[bb[0]] ?: 0).inc()
                    hm[bb[1]] = (hm[bb[1]] ?: 0).inc()
                    hm[bb[2]] = (hm[bb[2]] ?: 0).inc()
                    hm[bb[3]] = (hm[bb[3]] ?: 0).inc()
                }
            }

            hm.toSortedMap().forEach { (b, i) ->
                if (b != ZERO_BYTE) {
                    sb.append(
                        "${i.formatWithSeparator().padStart(16)} addresses occur ${
                            b.toString().padStart(8)
                        } times\n"
                    )
                }
            }
        }
        sb.append("Analysis took: ${t.inWholeSeconds.toString().padStart(8)} s")

        println(sb.toString())

        return Triple(sb.toString(), (hm[1] ?: 0L), t.inWholeSeconds)
    }
}

