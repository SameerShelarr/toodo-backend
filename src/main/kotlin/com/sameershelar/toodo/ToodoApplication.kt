package com.sameershelar.toodo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ToodoApplication

fun main(args: Array<String>) {
	runApplication<ToodoApplication>(*args)
}
