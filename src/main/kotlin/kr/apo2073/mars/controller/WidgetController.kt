package kr.apo2073.mars.controller

import com.google.gson.Gson
import com.google.gson.JsonObject
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

@Controller
class WidgetController {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val gson = Gson()

    @RequestMapping(value = ["/confirm"], method = [RequestMethod.POST])
    fun confirmPayment(@RequestBody jsonBody: String): ResponseEntity<JsonObject> {
        val requestData = gson.fromJson(jsonBody, JsonObject::class.java)
        val paymentKey = requestData.get("paymentKey").asString
        val orderId = requestData.get("orderId").asString
        val amount = requestData.get("amount").asString

        val obj = JsonObject().apply {
            addProperty("orderId", orderId)
            addProperty("amount", amount)
            addProperty("paymentKey", paymentKey)
        }

        val widgetSecretKey = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6"
        val encodedBytes = Base64.getEncoder().encode("$widgetSecretKey:".toByteArray(StandardCharsets.UTF_8))
        val authorizations = "Basic ${String(encodedBytes)}"

        val url = URL("https://api.tosspayments.com/v1/payments/confirm")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", authorizations)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.outputStream.use { it.write(gson.toJson(obj).toByteArray(Charsets.UTF_8)) }

        val code = connection.responseCode
        val isSuccess = code == 200
        val responseStream = if (isSuccess) connection.inputStream else connection.errorStream

        val jsonObject = gson.fromJson(InputStreamReader(responseStream, StandardCharsets.UTF_8), JsonObject::class.java)
        responseStream.close()

        val mars = try {
            amount.toInt() / 1000
        } catch (e: Exception) {
            0
        }

        jsonObject.addProperty("mars", mars)

        logger.info("✅ 결제 확인 완료 | orderId=$orderId | amount=$amount | mars=${mars}MARS | success=$isSuccess")

        return ResponseEntity.status(code).body(jsonObject)
    }

    @RequestMapping(value = ["/success"], method = [RequestMethod.GET])
    fun paymentRequest(
        request: HttpServletRequest,
        model: Model,
        @CookieValue(value = "user_uuid", required = false) uuid: String?
    ): String {
        return "/success"
    }

    @RequestMapping(value = ["/checkout"], method = [RequestMethod.GET])
    fun index(request: HttpServletRequest, model: Model): String {
        return "/checkout"
    }

    @RequestMapping(value = ["/fail"], method = [RequestMethod.GET])
    fun failPayment(request: HttpServletRequest, model: Model): String {
        val failCode = request.getParameter("code")
        val failMessage = request.getParameter("message")

        model.addAttribute("code", failCode)
        model.addAttribute("message", failMessage)

        return "/fail"
    }

}
