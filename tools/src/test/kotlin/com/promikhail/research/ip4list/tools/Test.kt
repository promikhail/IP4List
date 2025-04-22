package com.promikhail.research.ip4list.tools

import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import java.nio.CharBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

internal class Test {

    @Test
    fun testByteBuffer() = runTest {
        val ba = byteArrayOf(
            74, 8, 48, 3, 8,
            6, 4, 34, 16, 84,
            34, 65, 1, 56, 23
        )
        val bb = ByteBuffer.wrap(ba)
        bb.position(1)
        bb.limit(3)
        while (bb.hasRemaining()) {
            val b = bb.get()
            println(b)
        }
        assertEquals(0u, 0u)
    }

    @Test
    fun testReadAllLinesOfBuffer() = runTest {
        val s = "hello   \r\naloha  yo!\r\n\r\nDude wassup!!!\n How do you do??\r\nlast str"
        val buffer = stringToByteBuffer(s)

        val readLines = mutableListOf<String>()

        val ca = CharBuffer.allocate(17)

        buffer.readAllLinesOfBuffer(ca) { line ->
            readLines.add(line)
        }
        val referenceLines1 = listOf("hello   ", "aloha  yo!", "", "Dude wassup!!!", " How do you do??")
        assertContentEquals(referenceLines1, readLines)

        println()

        buffer.clear()
        buffer.limit(28)
        readLines.clear()

        buffer.readAllLinesOfBuffer(ca) { line ->
            readLines.add(line)
        }
        val referenceLines2 = listOf("hello   ", "aloha  yo!", "")
        assertContentEquals(referenceLines2, readLines)

    }

    @Test
    fun testGetFirstCrLfOffsetFromPosition() = runTest {
        val s = "hello   \r\naloha  yo!\r\n\r\nDude wassup!!!\n How do you do??\r\n\r\n"
        //       0           1          0

        val buffer = stringToByteBuffer(s)

        var cnt = buffer.getFirstCrLfOffsetFromPosition(0)

        assertEquals(10, cnt)
        assertEquals(0, buffer.position())

        cnt = buffer.getFirstCrLfOffsetFromPosition(23)

        assertEquals(24, cnt)
        assertEquals(0, buffer.position())

        cnt = buffer.getFirstCrLfOffsetFromPosition(24)

        assertEquals(39, cnt)
        assertEquals(0, buffer.position())
    }

    @Test
    fun testGetLastCrLfOffsetToEnd() = runTest {
        val s = "hello   \r\naloha  yo!\r\n\r\nDude wassup!!!\n How do you do??"
        val buffer = stringToByteBuffer(s)
        val cnt = buffer.getLastCrLfOffsetToEnd()
        assertEquals(16, cnt)
    }

    @Test
    fun testIncrementItemInByte() = runTest {
        val b1: Byte = 0b01101101

        var b2 = incrementBlock(b1, 1)
        assertEquals(b2, 0b10101101.toByte())

        b2 = incrementBlock(b1, 2)
        assertEquals(b2, 0b01111101.toByte())

        b2 = incrementBlock(b1, 3)
        b2 = incrementBlock(b2, 3)
        assertEquals(b2, 0b01101101.toByte())

        b2 = incrementBlock(b1, 4)
        assertEquals(b2, 0b01101110.toByte())
    }

    @Test
    fun testGetItemsValuesFromByte() = runTest {
        val b1: Byte = 0b01101101

        val bb = getItemsValuesFromByte(b1)

        assertEquals(0b01, bb[0])
        assertEquals(0b10, bb[1])
        assertEquals(0b11, bb[2])
        assertEquals(0b01, bb[3])
    }
}