package kr.apo2073.mars.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class User @JsonCreator constructor(
    @JsonProperty("id") var id: String,
    @JsonProperty("nickname") var nickname: String,
    @JsonProperty("coupons") var coupons: Int,
    @JsonProperty("createdAt") var createdAt: Long
)
