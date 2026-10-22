package com.oyl.frutossecos

import retrofit2.http.*

/** Contrato de integración. Requiere un servidor Spring Boot real; no se llama desde la demo offline. */
interface BackendApi {
 @GET("api/products") suspend fun products():List<ProductDto>
 @GET("api/products/{id}") suspend fun product(@Path("id") id:Int):ProductDto
 @POST("api/orders") suspend fun placeOrder(@Body order:CreateOrderDto):OrderDto
 @PATCH("api/orders/{id}/status") suspend fun changeStatus(@Path("id") id:Long,@Body status:StatusDto):OrderDto
 @PATCH("api/products/{id}/stock") suspend fun changeStock(@Path("id") id:Int,@Body movement:StockDto):ProductDto
}
data class ProductDto(val id:Int,val name:String,val category:String,val price:Int,val stock:Int)
data class CreateOrderDto(val clientId:Long,val delivery:String,val items:List<OrderItemDto>)
data class OrderItemDto(val productId:Int,val quantity:Int)
data class OrderDto(val id:Long,val status:String,val total:Int)
data class StatusDto(val status:String)
data class StockDto(val delta:Int)
