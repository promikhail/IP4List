package com.promikhail.research.ip4list.generator

import com.promikhail.research.ip4list.tools.LF
import com.promikhail.research.ip4list.tools.formatWithSeparator
import com.promikhail.research.ip4list.tools.genIpAddress
import com.promikhail.research.ip4list.tools.shardedArray.ShardedByteArray
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.RandomAccessFile
import java.lang.reflect.Method
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.random.Random
import kotlin.time.measureTime


class MappedGenerator : IGenerator {

    companion object {
        private const val MAPPED_FILE_BUFFER_SIZE = 64 * 1024 * 1024L
    }

    override fun generate(ipCount: Long, isCrLf: Boolean) = runBlocking {
        println("Start generating...")

        val byteArrayLF = byteArrayOf(LF)

        val rnd = Random(System.currentTimeMillis())
        val bm = ShardedByteArray()

        val fileName = buildString {
            append("files/")
            append("ip4list_")
            append(ipCount.formatWithSeparator())
            append(if (isCrLf) "_crlf" else "")
        }

        val f = File("$fileName.txt")
        f.delete()

        val raf = RandomAccessFile(f, "rw")
        raf.setLength(ipCount * MAX_IP_STR_SIZE_IN_BYTES)
        val channel = raf.channel

        val elapsedTime = measureTime {
            var ipCounter = 0L
            var bytesCounter = 0L

            var buffer: MappedByteBuffer? = null

            while (true) {
                buffer = channel.map(
                    FileChannel.MapMode.READ_WRITE,
                    bytesCounter,
                    MAPPED_FILE_BUFFER_SIZE
                )
                var bufferPosition = 0

                while (true) {
                    if (ipCounter == ipCount) break
                    val (uint, address) = genIpAddress(rnd)

                    bm.incBlockValue(uint)
                    val ipBytes = address.toByteArray(Charsets.US_ASCII)

                    if (bufferPosition + ipBytes.size + 1 > MAPPED_FILE_BUFFER_SIZE) break

                    buffer.put(bufferPosition, ipBytes)
                    bufferPosition += ipBytes.size

                    buffer.put(bufferPosition, byteArrayLF)
                    bufferPosition += 1

                    ipCounter++

                    bytesCounter += ipBytes.size + 1
                }

                buffer.force()
                unmap(buffer)

                if (ipCounter == ipCount) break
            }

            channel.truncate(bytesCounter)
            channel.close()
            raf.close()
        }

        val fsize = f.length() / 1024 / 1024

        val analysisResult = bm.analyze()

        val fstat = File("${fileName}_stat.txt")
        fstat.writer().apply {
            write("IP count = ${ipCount.formatWithSeparator()}\n\n")
            write("${analysisResult.first}\n\n")
            write("Generation took: ${elapsedTime}\n")
            write("File size: $fsize MB\n")
            flush()
        }

        println("Finished!")
    }

    private fun unmap(buffer: MappedByteBuffer) {
        try {
            val cleanerMethod: Method = buffer.javaClass.getMethod("cleaner")
            cleanerMethod.setAccessible(true)
            val cleaner: Any = cleanerMethod.invoke(buffer)
            val cleanMethod: Method = cleaner.javaClass.getMethod("clean")
            cleanMethod.invoke(cleaner)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}