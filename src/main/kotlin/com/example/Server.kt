package com.example

import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class Server {

    @McpTool(description = "add two numbers")
    fun plus(a: Int, b: Int): Int =
        a + b

}

fun main(args: Array<String>) { runApplication<Server>(*args) }
