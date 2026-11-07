package org.example

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.sql.DriverManager
import java.sql.Statement

@Serializable
data class OrderRequest(val item: String, val quantity: Int)

@Serializable
data class OrderResponse(val status: String, val orderId: Int?, val item: String, val quantity: Int, val message: String)

@Serializable
data class ErrorResponse(val error: String, val message: String)

fun main() {
    // 1. Initialize Local SQLite Database
    val connection = DriverManager.getConnection("jdbc:sqlite:shop.db")
    connection.createStatement().execute("""
        CREATE TABLE IF NOT EXISTS orders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            item TEXT,
            quantity INTEGER,
            status TEXT
        )
    """)
    println("Connected to local SQLite database.")

    // 2. Start Ktor Embedded Server on Port 3000
    embeddedServer(Netty, port = 3000) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            post("/api/v1/orders") {
                val request = try {
                    call.receive<OrderRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Bad Request", "Invalid JSON payload."))
                    return@post
                }

                println("[API Gateway / Router] Received POST request for /api/v1/orders")
                println("[Payload]: Item = ${request.item}, Quantity = ${request.quantity}")

                if (request.item.isBlank() || request.quantity <= 0) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Bad Request", "Item and valid quantity are required."))
                    return@post
                }

                // 3. Database Persistence
                val pstmt = connection.prepareStatement(
                    "INSERT INTO orders (item, quantity, status) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
                )
                pstmt.setString(1, request.item)
                pstmt.setInt(2, request.quantity)
                pstmt.setString(3, "CONFIRMED")
                pstmt.executeUpdate()

                val generatedKeys = pstmt.generatedKeys
                var orderId: Int? = null
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1)
                }

                // 4. Send Success Response
                println("[Success] Order saved with ID: $orderId")
                call.respond(HttpStatusCode.Created, OrderResponse(
                    status = "success",
                    orderId = orderId,
                    item = request.item,
                    quantity = request.quantity,
                    message = "Your order has been placed successfully!"
                ))
            }

            get("/api/v1/orders/{id}") {
                val orderId = call.parameters["id"]?.toIntOrNull()

                if (orderId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Bad Request", "Invalid JSON payload."))
                    return@get
                }

                println("[API Gateway] Received GET /api/v1/orders/$orderId")

                val pstmt = connection.prepareStatement(
                    "SELECT id, item, quantity, status FROM orders WHERE id = $orderId"
                )
                val resultSet = pstmt.executeQuery()

                if (resultSet.next()) {
                    val dbId = resultSet.getInt("id")
                    val item = resultSet.getString("item")
                    val quantity = resultSet.getInt("quantity")
                    val status = resultSet.getString("status")

                    call.respond(HttpStatusCode.OK, OrderResponse(
                        status = status,
                        orderId = dbId,
                        item = item,
                        quantity = quantity,
                        message = "Order retrieved successfully!"
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Not Found", "Order with ID $orderId does not exist."))
                }
            }
        }
    }.start(wait = true)
}