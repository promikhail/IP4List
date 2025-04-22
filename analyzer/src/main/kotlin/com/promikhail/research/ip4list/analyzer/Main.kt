package com.promikhail.research.ip4list.analyzer

import com.promikhail.research.ip4list.tools.MB
import com.promikhail.research.ip4list.tools.createFileChunksList
import com.promikhail.research.ip4list.tools.shardedArray.IShardedArray
import com.promikhail.research.ip4list.tools.shardedArray.ShardedTwoBitArray
import kotlinx.coroutines.*
import java.io.File
import kotlin.time.measureTimedValue


fun main() = runBlocking {
    val fileName = "ip4list_100_000.txt"
    val file = File("files/$fileName")

    if(!file.exists()) {
        println("File not exists!")
        return@runBlocking
    }

    val threadsCount = Runtime.getRuntime().availableProcessors()

    val sbm = ShardedTwoBitArray()
    //val sbm = ShardedByteArray()

    analyzeFile(file, threadsCount, sbm)
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun analyzeFile(file: File, threadsCount: Int, sbm: IShardedArray) {
    val fileSize = file.length()
    println("File size: $fileSize B, ${fileSize / MB} MB")

    val fileChunks = createFileChunksList(file, threadsCount)

    val res = measureTimedValue {
        coroutineScope {
            val asyncJobs = mutableListOf<Deferred<Long>>()
            for (fileChunk in fileChunks) {
                val job = async(Dispatchers.IO) {
                    processFileChunk(file, fileChunk, sbm)
                }
                asyncJobs.add(job)
            }
            asyncJobs.awaitAll()
            asyncJobs.sumOf { it.getCompleted() }
        }
    }

    println(
        "Total elapsed: ${res.duration.inWholeSeconds.toString().padStart(8)} s," +
                " totalLines=${res.value.toString().padStart(16)}\n"
    )

    println("Analyze started...\n")

    val analysisResults = sbm.analyze().second

    println("\nTotal unique IP address count: $analysisResults\n")

    println("Finished")
}