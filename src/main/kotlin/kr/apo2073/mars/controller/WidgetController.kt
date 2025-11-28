package kr.apo2073.mars.controller

import com.google.gson.Gson
import com.google.gson.JsonObject
import jakarta.servlet.http.HttpServletRequest
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.*

@Controller
class WidgetController {

    private val gson = Gson()

    data class PaymentRequest(
        val paymentKey: String,
        val orderId: String,
        val amount: String
    )

    @PostMapping("/confirm")
    @Throws(Exception::class)
    fun confirmPayment(@RequestBody jsonBody: String): ResponseEntity<JSONObject> {
        val parser = JSONParser()
        val requestData: JSONObject = try {
            parser.parse(jsonBody) as JSONObject
        } catch (e: ParseException) {
            throw RuntimeException(e) as Throwable
        }

        val paymentKey = requestData["paymentKey"] as String
        val orderId = requestData["orderId"] as String
        val amount = requestData["amount"] as String

        val obj = JSONObject().apply {
            put("orderId", orderId)
            put("amount", amount)
            put("paymentKey", paymentKey)
        }

        // TODO: 실제 시크릿 키로 변경
        val widgetSecretKey = "test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6"

        val authorizations = "Basic " + Base64.getEncoder()
            .encodeToString("$widgetSecretKey:".toByteArray(StandardCharsets.UTF_8))

        val url = URL("https://api.tosspayments.com/v1/payments/confirm")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", authorizations)
        connection.setRequestProperty("Content-Type", "application/json")
        connection.requestMethod = "POST"
        connection.doOutput = true

        connection.outputStream.use { it.write(obj.toString().toByteArray(StandardCharsets.UTF_8)) }

        val code = connection.responseCode
        val isSuccess = code == 200
        val responseStream = if (isSuccess) connection.inputStream else connection.errorStream

        val jsonObject = InputStreamReader(responseStream, StandardCharsets.UTF_8).use { reader ->
            parser.parse(reader) as JSONObject
        }
        responseStream.close()

        return ResponseEntity.status(code).body(jsonObject)
    }

    @GetMapping("/success")
    fun paymentRequest(request: HttpServletRequest, model: Model): String {
        return "success"
    }

    @GetMapping("/checkout")
    fun checkout(request: HttpServletRequest, model: Model): String {
        return "checkout"
    }

    @GetMapping("/fail")
    fun failPayment(request: HttpServletRequest, model: Model): String {
        val failCode = request.getParameter("code")
        val failMessage = request.getParameter("message")

        model.addAttribute("code", failCode)
        model.addAttribute("message", failMessage)

        return "fail"
    }
}