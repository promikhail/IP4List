package com.promikhail.research.ip4list.generator

import com.promikhail.research.ip4list.tools.formatWithSeparator
import com.promikhail.research.ip4list.tools.genIpAddress
import com.promikhail.research.ip4list.tools.shardedArray.ShardedByteArray
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.random.Random
import kotlin.time.measureTime


class Generator : IGenerator {

    companion object {
        const val IP_FLUSH_COUNT = 100_000
    }

    override fun generate(ipCount: Long, isCrLf: Boolean) = runBlocking {
        println("Start generating...")

        val rnd = Random(System.currentTimeMillis())

        val shardedMap = ShardedByteArray()

        val fileName = buildString {
            append("files/")
            append("ip4list_")
            append(ipCount.formatWithSeparator())
            append(if (isCrLf) "_crlf" else "")
        }

        val file = File("$fileName.txt")

        file.delete()
        file.parentFile.mkdirs()
        file.createNewFile()
        file.setWritable(true, false)

        val writer = file.bufferedWriter(bufferSize = IP_FLUSH_COUNT * MAX_IP_STR_SIZE_IN_BYTES)

        val sb = StringBuilder()

        val elapsedTime = measureTime {
            var flushCounter = 0
            for (i in 1..ipCount) {
                flushCounter++

                val (uint, address) = genIpAddress(rnd)

                shardedMap.incBlockValue(uint)

                sb.append(address)
                    .apply { if (isCrLf) append('\r') }
                    .append('\n')

                if (flushCounter == IP_FLUSH_COUNT) {
                    writer.write(sb.toString())
                    writer.flush()
                    flushCounter = 0
                    sb.clear()
                }
            }
        }
        writer.write(sb.toString())
        writer.flush()
        sb.clear()

        val fileSize = file.length() / 1024 / 1024

        writer.close()

        val analysisResult = shardedMap.analyze()

        val fstat = File("${fileName}_stat.txt")
        fstat.writer().apply {
            write("IP count = ${ipCount.formatWithSeparator()}\n\n")
            write("${analysisResult.first}\n\n")
            write("Generation took: ${elapsedTime}\n")
            write("File size: $fileSize MB\n")
            flush()
        }

        println("Finished!")
    }
}