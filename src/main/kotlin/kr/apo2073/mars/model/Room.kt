package kr.apo2073.mars.model

data class Room(
    var id: String = "",
    var q: Int = 0,
    var r: Int = 0,
    var size: String? = null,
    var price: Int = 0,
    var ownerId: String? = null,
    var desc: String? = null,
    var ownerNickname: String? = null
)
