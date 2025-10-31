package kr.apo2073.mars.model

data class User(
    val id: String,
    var nickname: String,
    var coupons: Int,
    val createdAt: Long = System.currentTimeMillis()
)
