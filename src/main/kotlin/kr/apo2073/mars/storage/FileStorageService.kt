package kr.apo2073.mars.storage

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service
class FileStorageService(
    private val mapper: ObjectMapper,
    @Value("\${app.data-dir}") private val dataDirPath: String
) {
    private val lock = ReentrantReadWriteLock()

    private fun file(name: String): File {
        val dir = File(dataDirPath)
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, name)
        if (!f.exists()) f.writeText("[]")
        return f
    }

    fun <T> readList(fileName: String, type: TypeReference<List<T>>): List<T> {
        lock.read {
            val f = file(fileName)
            return mapper.readValue(f, type)
        }
    }

    fun <T> writeList(fileName: String, list: List<T>) {
        lock.write {
            val f = file(fileName)
            val tmp = Files.createTempFile("tmp", null).toFile()
            mapper.writeValue(tmp, list)
            tmp.renameTo(f)
        }
    }
}
