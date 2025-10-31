package kr.apo2073.mars

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MarsHousingApplication

fun main(args: Array<String>) {
    runApplication<MarsHousingApplication>(*args)
}
