package com.promikhail.research.ip4list.tools

const val MB: Int = 1_048_576

const val CR: Byte = 13
const val LF: Byte = 10

/**
 * Размер буфера для итеративного чтения части файла.
 */
const val BUFFER_SIZE = 4 * 1024 * 1024

/**
 * Размер строки содержащей IP адрес максимальной длины.
 * Нужно для чтения дополнительной строки на стыках частей файла.
 */
const val BUFFER_EXTRA_SIZE = 17