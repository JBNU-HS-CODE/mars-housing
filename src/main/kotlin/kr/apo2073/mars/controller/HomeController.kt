package kr.apo2073.mars.controller

import kr.apo2073.mars.service.RoomService
import org.springframework.ui.Model
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController(private val roomService: RoomService) {

//    @GetMapping("/")
//    fun index(model: Model): String {
//        model.addAttribute("rooms", roomService.listAllRooms())
//        return "index"
//    }
}
