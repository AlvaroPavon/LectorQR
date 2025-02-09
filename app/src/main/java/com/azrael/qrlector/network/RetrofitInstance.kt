package com.azrael.qrlector.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Objeto Singleton que configura y proporciona la instancia de Retrofit para la comunicación con la API.
 *
 * Este objeto utiliza el patrón lazy para inicializar Retrofit y la interfaz [QrApi] únicamente cuando se necesiten,
 * garantizando así una inicialización eficiente.
 */
object RetrofitInstance {

    /**
     * URL base de la API.
     */
    private const val BASE_URL = "https://apiq2.onrender.com/"

    /**
     * Instancia de Retrofit configurada con la URL base y el convertidor Gson.
     *
     * Se inicializa de forma perezosa (lazy) para asegurar que la instancia se cree solo cuando se necesite.
     */
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Interfaz [QrApi] para acceder a los endpoints de la API.
     *
     * Se crea utilizando la instancia de Retrofit configurada.
     */
    val api: QrApi by lazy {
        retrofit.create(QrApi::class.java)
    }
}
