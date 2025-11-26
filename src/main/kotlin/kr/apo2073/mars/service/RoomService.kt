package kr.apo2073.mars.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kr.apo2073.mars.model.Room
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

@Service
class RoomService(private val userService: UserService) {

    private val rooms: MutableMap<String, Room> = mutableMapOf()
    private val lock = ReentrantReadWriteLock()
    private val objectMapper = ObjectMapper()

    init {
        loadDefaultRooms()
    }

    // 서버 시작 시 리소스에서 초기 JSON 로딩
    private fun loadDefaultRooms() {
        lock.write {
            try {
                val json = javaClass.classLoader
                    .getResourceAsStream("init/rooms.json")
                    ?.reader(Charsets.UTF_8)?.readText()
                    ?: throw Exception("init/rooms.json not found")

                if (json.isNotBlank()) {
                    val mapType = object : TypeReference<Map<String, Room>>() {}
                    val loaded: Map<String, Room> = objectMapper.readValue(json, mapType)
                    rooms.clear()
                    loaded.forEach { (id, room) ->
                        room.id = id
                        rooms[id] = room
                    }
                }
            } catch (e: Exception) {
                println("🚨 초기 rooms.json 로딩 오류: ${e.message}")
            }
        }
    }

    fun saveRooms() {
        lock.read {
            try {
                val file = File("/data/rooms.json")
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms)
            } catch (e: Exception) {
                println("🚨 rooms.json 쓰기 오류: ${e.message}")
            }
        }
    }

    fun listAllRooms(): Map<String, Room> = lock.read { rooms.toMap() }

    fun listAllRoomsWithOwnerNicknames(): Map<String, Room> {
        lock.read {
            rooms.values.forEach { room ->
                room.ownerNickname = room.ownerId?.let { userService.getUser(it)?.nickname } ?: "알 수 없는 사용자"
            }
            return rooms.toMap()
        }
    }

    fun findById(id: String): Room? = lock.read { rooms[id] }

    fun searchRooms(query: String): Map<String, Room> {
        if (query.isBlank()) return listAllRooms()
        val q = query.lowercase().trim()
        return lock.read {
            rooms.filter { (id, room) ->
                id.lowercase().contains(q) ||
                        (room.desc?.lowercase()?.contains(q) ?: false) ||
                        (room.size?.lowercase()?.contains(q) ?: false)
            }
        }
    }

    fun purchaseRoom(roomId: String, userId: String): Boolean {
        lock.write {
            val room = rooms[roomId] ?: return false
            val user = userService.getUser(userId) ?: return false
            if (room.ownerId != null || user.coupons < room.price) return false
            room.ownerId = user.id
            room.ownerNickname = user.nickname
            user.coupons -= room.price
            userService.saveUsers()
            return true
        }
    }
}
