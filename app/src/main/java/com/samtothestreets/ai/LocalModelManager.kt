package com.samtothestreets.ai

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ModelMode(val displayName: String) {
    BUILT_IN("Built-in AI"),
    USER_MODEL("User Model"),
    RULES_ONLY("Rules Only")
}

data class DiagnosticsState(
    val liteRtLmActive: Boolean = false,
    val backend: String = "CPU",
    val builtInFound: Boolean = false,
    val copiedFromAssets: Boolean = false,
    val loadSuccess: Boolean = false,
    val activeMode: ModelMode = ModelMode.RULES_ONLY,
    val activeFilename: String? = null,
    val exactRuntimeModelPath: String? = null,
    val deviceStatusExplanation: String = "Initializing AI Check...",
    val lastError: String? = null
)

object LocalModelManager {
    private val _diagnosticsFlow = MutableStateFlow(DiagnosticsState())
    val diagnosticsFlow: StateFlow<DiagnosticsState> = _diagnosticsFlow.asStateFlow()

    val diagnostics: DiagnosticsState
        get() = _diagnosticsFlow.value

    val isModelLoaded: Boolean
        get() = diagnostics.activeMode != ModelMode.RULES_ONLY && diagnostics.loadSuccess

    private var engine: Engine? = null
    private var activeConversation: com.google.ai.edge.litertlm.Conversation? = null

    fun resetSession() {
        try {
            activeConversation?.close()
        } catch (e: Throwable) {
            Log.w("AILoader", "Error closing session: ${e.message}")
        } finally {
            activeConversation = null
        }
    }

    suspend fun tryAutoLoadBundledModel(context: Context, isManualRetry: Boolean = false) = withContext(Dispatchers.IO) {
        if (diagnostics.activeMode == ModelMode.USER_MODEL) return@withContext

        val prefs = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)

        if (isManualRetry) {
            prefs.edit().putBoolean("has_crashed_loading_ai", false).apply()
        }

        if (prefs.getBoolean("is_loading_ai", false)) {
            prefs.edit()
                .putBoolean("has_crashed_loading_ai", true)
                .putBoolean("is_loading_ai", false)
                .apply()
        }

        if (prefs.getBoolean("has_crashed_loading_ai", false)) {
            _diagnosticsFlow.value = DiagnosticsState(
                activeMode = ModelMode.RULES_ONLY,
                deviceStatusExplanation = "Native crash detected previously. Device memory/hardware incompatible.",
                lastError = "Fatal hardware abort safely bypassed. Fallback to Rules Mode."
            )
            return@withContext
        }

        val success1B = attemptLoadModel(context, "ai_model/gemma3-1b-it-int4.litertlm", "gemma3-1b-it-int4.litertlm")
        if (success1B) return@withContext
        
        val success270M = attemptLoadModel(context, "ai_model/gemma3-270m-it-q8.litertlm", "gemma3-270m-it-q8.litertlm")
        if (success270M) return@withContext

        forceRulesOnly("Both models failed to load. See last error for details.")
    }

    private suspend fun attemptLoadModel(context: Context, assetPath: String, fileName: String): Boolean {
        _diagnosticsFlow.value = _diagnosticsFlow.value.copy(
            deviceStatusExplanation = "Checking for packaged model $fileName..."
        )
        
        var found = false
        var copied = false
        val extractedFile = File(context.filesDir, fileName)
        val prefs = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)

        try {
            val assetList = context.assets.list("ai_model")
            if (assetList?.contains(fileName) == true) {
                found = true
            } else {
                return false // Move to next fallback
            }

            if (!extractedFile.exists() || extractedFile.length() == 0L) {
                _diagnosticsFlow.value = _diagnosticsFlow.value.copy(
                    deviceStatusExplanation = "Extracting model. Please wait..."
                )
                context.assets.open(assetPath).use { inputStream ->
                    FileOutputStream(extractedFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                copied = true
            }

            _diagnosticsFlow.value = _diagnosticsFlow.value.copy(
                deviceStatusExplanation = "Loading $fileName into memory..."
            )

            prefs.edit().putBoolean("is_loading_ai", true).commit()

            val config = EngineConfig(modelPath = extractedFile.absolutePath, backend = Backend.CPU())
            
            engine?.close()
            resetSession() // Ensure active conversation is nuked
            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
            
            prefs.edit().putBoolean("is_loading_ai", false).apply()
            
            _diagnosticsFlow.value = DiagnosticsState(
                liteRtLmActive = true,
                backend = "CPU",
                builtInFound = found,
                copiedFromAssets = copied,
                loadSuccess = true,
                activeMode = ModelMode.BUILT_IN,
                activeFilename = fileName,
                exactRuntimeModelPath = extractedFile.absolutePath,
                deviceStatusExplanation = "AI is ready and loaded.",
                lastError = null
            )
            return true
        } catch (e: Throwable) {
            prefs.edit().putBoolean("is_loading_ai", false).apply()
            Log.e("AILoader", "Failed to load $fileName.", e)
            engine?.close()
            engine = null
            _diagnosticsFlow.value = DiagnosticsState(
                builtInFound = found,
                copiedFromAssets = copied,
                loadSuccess = false,
                activeMode = ModelMode.RULES_ONLY,
                activeFilename = null,
                deviceStatusExplanation = "Device may not support $fileName.",
                lastError = "Failure Reason: ${e.message}"
            )
            return false
        }
    }

    suspend fun loadModelFromUri(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
        try {
            val userFile = File(context.filesDir, "litertlm_user_model.litertlm")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(userFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw Exception("Could not open model stream.")

            prefs.edit().putBoolean("is_loading_ai", true).commit()

            val config = EngineConfig(modelPath = userFile.absolutePath, backend = Backend.CPU())
            engine?.close()
            resetSession()
            val newEngine = Engine(config)
            newEngine.initialize()
            engine = newEngine
            
            prefs.edit().putBoolean("is_loading_ai", false).apply()

            val filename = uri.lastPathSegment ?: "Local User Model"
            
            _diagnosticsFlow.value = DiagnosticsState(
                liteRtLmActive = true,
                backend = "CPU",
                loadSuccess = true,
                activeMode = ModelMode.USER_MODEL,
                activeFilename = filename,
                exactRuntimeModelPath = userFile.absolutePath,
                deviceStatusExplanation = "Custom User Model loaded successfully.",
                lastError = null
            )
            
            Result.success(filename)
        } catch (e: Throwable) {
            prefs.edit().putBoolean("is_loading_ai", false).apply()
            Log.e("AILoader", "Failed to load custom user model.", e)
            engine?.close()
            engine = null
            _diagnosticsFlow.value = _diagnosticsFlow.value.copy(
                liteRtLmActive = false,
                loadSuccess = false,
                activeMode = ModelMode.RULES_ONLY,
                deviceStatusExplanation = "Custom model failed. Device limits or corrupt file.",
                lastError = "Failure Reason: ${e.message}"
            )
            Result.failure(e)
        }
    }

    fun forceRulesOnly(reason: String = "Manually forced to Rules Mode.") {
        engine?.close()
        engine = null
        resetSession()
        _diagnosticsFlow.value = _diagnosticsFlow.value.copy(
            liteRtLmActive = false,
            loadSuccess = false,
            activeMode = ModelMode.RULES_ONLY,
            activeFilename = null,
            deviceStatusExplanation = reason,
            lastError = if (reason.contains("force")) "User enacted manual override to Rules Only." else _diagnosticsFlow.value.lastError
        )
    }
    
    fun generateResponse(prompt: String, contextInfo: String = "Unknown Context"): String {
        val currentEngine = engine ?: return "[FAILURE] Model not active."
        return try {
            if (activeConversation == null) {
                activeConversation = currentEngine.createConversation()
            }
            activeConversation!!.sendMessage(prompt).toString()
        } catch (e: Throwable) {
            val errorStr = e.message ?: "Unknown Engine Error"
            if (errorStr.contains("FAILED_PRECONDITION") || errorStr.contains("session")) {
                val cleanupAttempted = activeConversation != null
                resetSession()
                
                try {
                    activeConversation = currentEngine.createConversation()
                    activeConversation!!.sendMessage(prompt).toString()
                } catch (retryE: Throwable) {
                    val finalErr = retryE.message ?: "Unknown"
                    Log.e("AILoader", "Session Creation Failed. Context: $contextInfo, Alive Before Cleanup: $cleanupAttempted, Cleanup Attempted: true, Error: $finalErr")
                    "Sorry, the AI engine got tangled up and couldn't reset cleanly. Please close and reopen this graph screen to clear it!"
                }
            } else {
                Log.e("AILoader", "Generation Failed. Context: $contextInfo, Error: $errorStr")
                "Sorry, the AI encountered a math or processing error and couldn't answer." // Typo intentional per request? No wait, user asked for exactly 1 typo natively in the specific texts (About/Onboarding/Aces/Credits). I shouldn't force typos here unless needed. I'll just keep it clean.
            }
        }
    }
}
