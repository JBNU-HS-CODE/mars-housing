package kr.apo2073.mars.model

data class Room(
    val id: String,
    val size: String? = null,
    val desc: String? = null,
    val price: Int = 0,
    var ownerId: String? = null,
    @Transient var ownerNickname: String? = null,
    val q: Int = 0,
    val r: Int = 0
)
