package com.promikhail.research.ip4list.tools

import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.FileChannel


fun FileChannel.moveChannelPositionToFirstNewLine(searchStartPosition: Long) {
    val buffer = ByteBuffer.allocate(BUFFER_EXTRA_SIZE)
    this.position(searchStartPosition)
    this.read(buffer)
    val offset = buffer.getFirstCrLfOffsetFromPosition(0)
    val newPosition = searchStartPosition + offset
    this.position(newPosition)
}

fun ByteBuffer.getLastCrLfOffsetToEnd(): Int {
    for (i in this.limit() - 1 downTo this.position()) {
        if (this.get(i) == LF) return this.limit() - i - 1
    }
    return -1
}

fun ByteBuffer.getFirstCrLfOffsetFromPosition(searchStartPosition: Int): Int {
    for (i in searchStartPosition until this.limit()) {
        if (this.get(i) == CR) return i + 2
        if (this.get(i) == LF) return i + 1
    }
    return -1
}

suspend fun ByteBuffer.readAllLinesOfBuffer(ca: CharBuffer, action: suspend (String) -> Unit) {
    ca.clear()
    var b: Byte
    while (this.hasRemaining()) {
        b = this.get()
        if (b == CR || b == LF) {
            ca.flip()
            action(ca.toString())
            ca.clear()
            if (b == CR) {
                this.get()
            }
        } else {
            ca.put(Char(b.toInt()))
        }
    }
}

fun Long.formatWithSeparator(separator: String = "_"): String {
    return this.toString()
        .reversed()
        .chunked(3)
        .joinToString(separator)
        .reversed()
}