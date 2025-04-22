package com.promikhail.research.ip4list.tools

import java.io.File
import kotlin.math.ceil

data class FileChunk(
    val num: Int,
    val startPos: Long,
    val endPos: Long,
)

fun createFileChunksList(file: File, chunkCount: Int): List<FileChunk> {
    require(chunkCount in 1..32)
    val fileSize = file.length()
    val fileChunkSize = ceil(fileSize.toDouble().div(chunkCount)).toLong()

    val res = mutableListOf<FileChunk>()

    val lastPosition = fileSize - 1

    var startPosition = 0L
    var endPosition = fileChunkSize

    for (i in 1..chunkCount) {
        res.add(
            FileChunk(
                i,
                startPosition,
                endPosition,
            )
        )
        startPosition = endPosition + 1
        endPosition = (endPosition + fileChunkSize).coerceAtMost(lastPosition)
    }

    return res
}