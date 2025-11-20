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

    /** 파일 객체 가져오기. 없으면 생성 */
    private fun getFile(): File {
        val dir = File("/workspace/data")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)
        if (!file.exists()) file.createNewFile()
        return file
    }

    /** users.json 로드 */
    private fun loadUsers() {
        lock.write {
            try {
                val file = getFile()
                val text = file.readText()
                if (text.isNotBlank()) {
                    val list: List<User> = objectMapper.readValue(
                        text,
                        object : TypeReference<List<User>>() {}
                    )
                    users.clear()
                    list.forEach { users[it.id] = it }
                }
            } catch (e: Exception) {
                println("🚨 users.json 읽기 오류: ${e.message}")
            }
        }
    }

    /** users.json 저장 */
    fun saveUsers() {
        lock.write {
            try {
                if (users.isEmpty()) return  // 빈 데이터는 저장하지 않음
                val file = getFile()
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, users.values.toList())
            } catch (e: Exception) {
                println("🚨 users.json 쓰기 오류: ${e.message}")
            }
        }
    }

    /** UUID로 유저 조회 */
    fun getUser(id: String): User? = lock.read { users[id] }

    /** 존재하면 조회, 없으면 생성. 생성 시 기본 닉네임/쿠폰 적용 */
    fun getOrCreateUser(id: String, defaultNickname: String = "Guest_Mars", defaultCoupons: Int = 10): User {
        val user = lock.write {
            users.getOrPut(id) {
                User(
                    id = id,
                    nickname = defaultNickname,
                    coupons = defaultCoupons,
                    createdAt = System.currentTimeMillis()
                )
            }
        }
        saveUsers() // write 후 안전하게 저장
        return user
    }

    /** 모든 유저 리스트 */
    fun listAll(): List<User> = lock.read { users.values.toList() }

    /** 특정 유저에게 쿠폰 추가 */
    fun addCoupons(id: String, amount: Int): Boolean {
        if (amount <= 0) return false
        val user = lock.write { users[id] } ?: return false
        user.coupons += amount
        saveUsers()
        return true
    }
}
