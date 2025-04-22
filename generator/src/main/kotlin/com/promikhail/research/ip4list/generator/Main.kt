package com.promikhail.research.ip4list.generator

import com.promikhail.research.ip4list.tools.getArgsMap


fun main(args: Array<String>) {
    val m = getArgsMap(args)

    val ipCount = 5_000_000_000L
    val isCrLf = false

    if (m["mode"] == "mapped") {
        val generator = MappedGenerator()
        generator.generate(ipCount, isCrLf)
    } else {
        val generator = Generator()
        generator.generate(ipCount, isCrLf)
    }
}