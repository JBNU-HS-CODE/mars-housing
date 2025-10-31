package kr.apo2073.mars.controller

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import kr.apo2073.mars.model.Room
import kr.apo2073.mars.model.User
import kr.apo2073.mars.service.RoomService
import kr.apo2073.mars.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.util.*

@Controller
class RoomController(
    private val roomService: RoomService,
    private val userService: UserService
) {
    private val defaultNickname = "Guest_Mars"
    private val defaultCoupons = 10

    private fun getOrCreateUser(uuidCookie: String?, response: HttpServletResponse): User {
        val userUuid = try {
            uuidCookie?.let { UUID.fromString(it) } ?: UUID.randomUUID()
        } catch (e: Exception) {
            UUID.randomUUID()
        }
        val user = userService.getOrCreateUser(userUuid.toString(), defaultNickname, defaultCoupons)
        response.addCookie(Cookie("user_uuid", user.id).apply { maxAge = 60*60*24*365 })
        return user
    }

    @GetMapping("/")
    fun index(@CookieValue(value = "user_uuid", required = false) uuid: String?,
              model: Model,
              response: HttpServletResponse): String {
        val user = getOrCreateUser(uuid, response)
        val rooms = roomService.listAllRoomsWithOwnerNicknames()
        model.addAttribute("rooms", rooms)
        model.addAttribute("userUuid", user.id)
        model.addAttribute("userNickname", user.nickname)
        model.addAttribute("userCoupons", user.coupons)
        model.addAttribute("roomsJson", roomsServiceJsonString(rooms))
        return "index"
    }

    @GetMapping("/search")
    fun search(@RequestParam("q", required = false) query: String?,
               @CookieValue(value = "user_uuid", required = false) uuid: String?,
               model: Model,
               response: HttpServletResponse): String {
        val user = getOrCreateUser(uuid, response)
        val filtered = roomService.searchRooms(query ?: "")
        model.addAttribute("rooms", filtered)
        model.addAttribute("userUuid", user.id)
        model.addAttribute("userNickname", user.nickname)
        model.addAttribute("userCoupons", user.coupons)
        model.addAttribute("searchQuery", query)
        model.addAttribute("roomsJson", roomsServiceJsonString(filtered))
        return "index"
    }

    @PostMapping("/change-nickname")
    fun changeNickname(@RequestParam nickname: String,
                       @CookieValue(value = "user_uuid") uuid: String,
                       redirect: RedirectAttributes): String {
        val user = userService.getUser(uuid) ?: return "redirect:/"
        if (nickname.trim().length < 2) {
            redirect.addFlashAttribute("flashMessages", listOf(mapOf("category" to "error", "message" to "닉네임은 두 글자 이상이어야 합니다.")))
            return "redirect:/"
        }
        user.nickname = nickname.trim()
        userService.saveUsers()
        redirect.addFlashAttribute("flashMessages", listOf(mapOf("category" to "success", "message" to "닉네임 변경 완료: ${user.nickname}")))
        return "redirect:/"
    }

    @PostMapping("/purchase-room/{roomId}")
    fun purchaseRoom(@PathVariable roomId: String,
                     @CookieValue(value = "user_uuid") uuid: String,
                     redirect: RedirectAttributes): String {
        val user = userService.getUser(uuid) ?: return "redirect:/"
        val room = roomService.findById(roomId) ?: run {
            redirect.addFlashAttribute("flashMessages", listOf(mapOf("category" to "error", "message" to "존재하지 않는 방입니다.")))
            return "redirect:/"
        }
        if (room.ownerId != null) {
            redirect.addFlashAttribute("flashMessages", listOf(mapOf("category" to "error", "message" to "이미 구매된 방입니다.")))
            return "redirect:/"
        }
        if (user.coupons < room.price) {
            redirect.addFlashAttribute("flashMessages", listOf(mapOf("category" to "error", "message" to "코인이 부족합니다.")))
            return "redirect:/"
        }
        room.ownerId = user.id
        user.coupons -= room.price
        roomService.saveRooms()
        userService.saveUsers()
        redirect.addFlashAttribute("flashMessages", listOf(mapOf("category" to "success", "message" to "방 ${room.id} 구매 성공!")))
        return "redirect:/"
    }

    @GetMapping("/users")
    fun usersList(
        @CookieValue(value = "user_uuid", required = false) uuid: String?,
        model: Model,
        response: HttpServletResponse
    ): String {
        val user = getOrCreateUser(uuid, response)

        val allUsers = userService.listAll().map { u ->
            val ownedRooms = roomService.listAllRooms().filter { it.value.ownerId == u.id }.keys.toList()
            u to ownedRooms
        }

        val userListForView = allUsers.map { (u, rooms) ->
            mapOf(
                "id" to u.id,
                "nickname" to u.nickname,
                "coupons" to u.coupons,
                "ownedRooms" to rooms
            )
        }

        model.addAttribute("userList", userListForView)
        model.addAttribute("currentNickname", user.nickname)
        model.addAttribute("currentCoupons", user.coupons)

        return "users"
    }


    private fun roomsServiceJsonString(rooms: Map<String, Room>): String {
        val mapper = com.fasterxml.jackson.databind.ObjectMapper()
        return mapper.writeValueAsString(rooms)
    }
}
