package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject

data class Message(
    val role: Role,
    val text: String
)

enum class Role {
    USER, AI, SYSTEM, ERROR
}

data class TerminalState(
    val history: List<Message> = listOf(
        Message(Role.SYSTEM, "CONEXIÓN ESTABLECIDA.\nINGRESE CONSULTA PARA LA UNIDAD CENTRAL DE PROCESAMIENTO.")
    ),
    val isGenerating: Boolean = false,
    val currentAiText: String = "",
    val status: String = "EN ESPERA"
)

class TerminalViewModel : ViewModel() {
    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    // Keep track of full history for API context
    private val apiHistory = mutableListOf<Content>()

    fun processCommand(command: String) {
        if (_state.value.isGenerating || command.isBlank()) return

        val userMessage = Message(Role.USER, command)
        _state.value = _state.value.copy(
            history = _state.value.history + userMessage,
            isGenerating = true,
            status = "PROCESANDO...",
            currentAiText = ""
        )

        apiHistory.add(Content(role = "user", parts = listOf(Part(text = command))))

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    handleError("ERROR: API KEY NO CONFIGURADA.")
                    return@launch
                }

                val request = GenerateContentRequest(
                    contents = apiHistory.toList(),
                    systemInstruction = Content(role = "system", parts = listOf(Part(text = "Responde de forma concisa, directa y con un tono ligeramente mecánico o de sistema computacional antiguo si es apropiado."))),
                    tools = listOf(
                        com.example.network.Tool(googleSearch = buildJsonObject {})
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "RESPUESTA VACÍA O NO RECONOCIDA."

                apiHistory.add(Content(role = "model", parts = listOf(Part(text = responseText))))
                
                typeWriterEffect(responseText)

            } catch (e: retrofit2.HttpException) {
                val errorBody = e.response()?.errorBody()?.string() ?: e.message
                handleError("ERROR DE SISTEMA (HTTP ${e.code()}): $errorBody")
                apiHistory.removeLastOrNull()
            } catch (e: Exception) {
                handleError("ERROR DE SISTEMA: No se pudo establecer comunicación. Intente nuevamente. (${e.message})")
                apiHistory.removeLastOrNull() // Remove the last user message from API history on fail
            }
        }
    }

    private suspend fun typeWriterEffect(text: String) {
        _state.value = _state.value.copy(currentAiText = "")
        val charArray = text.toCharArray()
        
        for (i in charArray.indices) {
            _state.value = _state.value.copy(
                currentAiText = _state.value.currentAiText + charArray[i]
            )
            // Simulate variable typing speed
            delay((15..30).random().toLong())
        }

        // Add the finished message to history
        val aiMessage = Message(Role.AI, _state.value.currentAiText)
        _state.value = _state.value.copy(
            history = _state.value.history + aiMessage,
            isGenerating = false,
            status = "EN ESPERA",
            currentAiText = ""
        )
    }

    private fun handleError(errorText: String) {
        val errorMessage = Message(Role.ERROR, errorText)
        _state.value = _state.value.copy(
            history = _state.value.history + errorMessage,
            isGenerating = false,
            status = "EN ESPERA",
            currentAiText = ""
        )
    }
}
