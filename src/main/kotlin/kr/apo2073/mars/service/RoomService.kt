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

    private val fileName = "rooms.json"
    private val rooms: MutableMap<String, Room> = mutableMapOf()
    private val lock = ReentrantReadWriteLock()
    private val objectMapper = ObjectMapper()

    init {
        loadRooms()
    }

    private fun getFile(): File {
        val file = File("data", fileName)
        if (!file.exists()) file.createNewFile()
        return file
    }

    private fun loadRooms() {
        lock.write {
            try {
                val file = getFile()
                if (!file.exists()) file.createNewFile()
                if (file.readText().isNotBlank()) {
                    val list: List<Room> = objectMapper.readValue(file, object : TypeReference<List<Room>>() {})
                    list.forEach { rooms[it.id] = it }
                }
            } catch (e: Exception) {
                println("🚨 rooms.json 읽기 오류: ${e.message}")
            }
        }
    }

    fun saveRooms() {
        lock.write {
            try {
                val file = getFile()
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, rooms.values.toList())
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
            user.coupons -= room.price
            saveRooms()
            userService.saveUsers()
            return true
        }
    }
}
