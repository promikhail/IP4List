package com.promikhail.research.ip4list.analyzer

import com.promikhail.research.ip4list.tools.*
import com.promikhail.research.ip4list.tools.shardedArray.IShardedArray
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.FileChannel
import kotlin.time.measureTimedValue


suspend fun processFileChunk(f: File, fileChunk: FileChunk, bm: IShardedArray): Long {
    println(
        "PROCESS fileChunkNum=${fileChunk.num.toString().padStart(3)}, "
                + "startPos=${fileChunk.startPos.toString().padStart(16)}, "
                + "endPos=${fileChunk.endPos.toString().padStart(16)}, "
                + "thread=${Thread.currentThread().threadId()}"
    )

    val raf = RandomAccessFile(f, "r")
    val chan = raf.channel

    val timing = measureTimedValue {
        var linesRead = 0L

        readFileChunk(chan, fileChunk) { s ->
            linesRead++

            val ipInt = try {
                ipStrToUInt(s)
            } catch (ex: Exception) {
                println("Wrong IP on fileChunk=${fileChunk.num}, IP=$s")
                throw ex
            }

            bm.incBlockValue(ipInt)
        }
        linesRead
    }

    chan.close()
    raf.close()

    println(
        "PROCESS #${fileChunk.num.toString().padStart(3)} FINISHED! "
                + "Bytes processed: ${(fileChunk.endPos - fileChunk.startPos + 1).toString().padStart(16)}, "
                + "linesRead=${timing.value.toString().padStart(16)}, "
                + "elapsed=${timing.duration.inWholeSeconds.toString().padStart(8)} s"
    )

    return timing.value
}

suspend fun readFileChunk(channel: FileChannel, fileChunk: FileChunk, action: suspend (String) -> Unit) {
    if (fileChunk.num == 1) {
        channel.position(0)
    } else {
        channel.moveChannelPositionToFirstNewLine(fileChunk.startPos - 1)
    }

    val buffer = ByteBuffer.allocate(BUFFER_SIZE)
    val bufferWithExtra = ByteBuffer.allocate(BUFFER_SIZE + BUFFER_EXTRA_SIZE)

    val ca = CharBuffer.allocate(17)

    while (true) {
        val channelStart = channel.position()

        if (channelStart + BUFFER_SIZE + 1 > fileChunk.endPos) {
            bufferWithExtra.clear()
            val bytesRead = channel.read(bufferWithExtra)
            if (bytesRead == -1) break
            bufferWithExtra.rewind()

            var offset = bufferWithExtra.getFirstCrLfOffsetFromPosition((fileChunk.endPos - channelStart).toInt())
            if (offset != -1) {
                bufferWithExtra.limit(offset)
            }

            bufferWithExtra.readAllLinesOfBuffer(ca) {
                action(it)
            }
            break
        } else {
            buffer.clear()
            val bytesRead = channel.read(buffer)
            if (bytesRead == -1) break
            buffer.rewind()

            val offset = buffer.getLastCrLfOffsetToEnd()
            channel.position(channel.position() - offset)
            buffer.limit(bytesRead - offset)

            buffer.readAllLinesOfBuffer(ca) {
                action(it)
            }
        }
    }
}