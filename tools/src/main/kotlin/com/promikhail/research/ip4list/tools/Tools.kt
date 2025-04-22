package com.promikhail.research.ip4list.tools

import java.nio.ByteBuffer
import kotlin.random.Random
import kotlin.random.nextUInt


fun genIpAddress(rnd: Random): Pair<UInt, String> {
    val uint = rnd.nextUInt()
    val b1 = (uint shr 24).toUByte()
    val b2 = (uint shr 16).toUByte()
    val b3 = (uint shr 8).toUByte()
    val b4 = uint.toUByte()
    return uint to "$b1.$b2.$b3.$b4"
}

fun ipStrToUInt(ipStr: String): UInt {
    var s: String = ""
    var j: Int = 0
    var ip: UInt = 0u

    for (c in ipStr) {
        if (c != '.') {
            s += c
            continue
        }
        j++
        ip += s.toUByte().toUInt() shl (8 * (4 - j))
        s = ""
    }
    if (j != 3) throw IllegalArgumentException("Wrong IP string: $ipStr")
    ip += s.toUByte().toUInt()

    return ip
}

fun getArgsMap(args: Array<String>): Map<String, String> {
    return args
        .filter { s ->
            s.count { it == '=' } == 1
        }
        .associate {
            val p = it.split('=')
            p[0] to p[1]
        }
}

fun stringToByteBuffer(s: String): ByteBuffer {
    return ByteBuffer.wrap(s.encodeToByteArray())
}

/**
 * Прибавляет 1 к указанной паре битов в Byte.
 * @param inByte исходное значение Byte
 * @param itemNumber номер пары битов (1 для битов 8-7, ..., 4 для битов 1-2)
 * @return новое значение Byte с инкрементированной парой битов
 */
fun incrementBlock(inByte: Byte, itemNumber: Int): Byte {
    require(itemNumber in 1..4) { "Value $itemNumber not in 1..4" }

    val buint = inByte.toUInt()

    val shift = 8 - (itemNumber * 2)
    val mask = 0b11u shl shift
    val bits = (buint and mask) shr shift

    if (bits == 0b11u) return inByte

    val newBits = (bits + 1u) and 0b11u

    val clearedBits = buint and mask.inv()

    return ((newBits shl shift) or clearedBits).toByte()
}

fun getItemsValuesFromByte(b: Byte): ByteArray {
    val i = b.toUInt()
    val mask1 = ((i and 0b11000000u) shr 6).toByte()
    val mask2 = ((i and 0b00110000u) shr 4).toByte()
    val mask3 = ((i and 0b00001100u) shr 2).toByte()
    val mask4 = ((i and 0b00000011u)).toByte()
    return byteArrayOf(mask1, mask2, mask3, mask4)
}

fun printBuffer(buffer: ByteBuffer): String {
    var i = buffer.position()
    val sb = StringBuilder()
    var b: Byte
    sb.append('[')
    while (i < buffer.limit()) {
        b = buffer.get(i)
        sb.append(b).append(',')
        if (i.rem(4) == 0) {
            sb.append(' ')
        }
        if (b == LF) {
            sb.append("   *   ")
        }
        i++
    }
    sb.append(']')
    return sb.toString()
}
