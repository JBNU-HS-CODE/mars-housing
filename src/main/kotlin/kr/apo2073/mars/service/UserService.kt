package kr.apo2073.mars.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.apo2073.mars.model.User
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service
class UserService {

    private val fileName = "users.json"
    private val users: MutableMap<String, User> = mutableMapOf()
    private val lock = ReentrantReadWriteLock()
    private val objectMapper = ObjectMapper()

    init {
        loadUsers()
    }

    private fun getFile(): File {
        val dir = File("data")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        if (!file.exists()) file.createNewFile()
        return file
    }

    private fun loadUsers() {
        lock.write {
            try {
                val file = getFile()
                if (!file.exists()) file.createNewFile()
                if (file.readText().isNotBlank()) {
                    val list: List<User> = objectMapper.readValue(file, object : TypeReference<List<User>>() {})
                    list.forEach { users[it.id] = it }
                }
            } catch (e: Exception) {
                println("🚨 users.json 읽기 오류: ${e.message}")
            }
        }
    }

    fun saveUsers() {
        lock.write {
            try {
                val file = getFile()
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, users.values.toList())
            } catch (e: Exception) {
                println("🚨 users.json 쓰기 오류: ${e.message}")
            }
        }
    }

    fun getUser(id: String): User? = lock.read { users[id] }

    fun getOrCreateUser(id: String, defaultNickname: String = "Guest_Mars", defaultCoupons: Int = 10): User {
        return lock.write {
            users.getOrPut(id) {
                val newUser = User(id = id, nickname = defaultNickname, coupons = defaultCoupons)
                saveUsers()
                newUser
            }
        }
    }

    fun listAll(): List<User> = lock.read { users.values.toList() }
}
