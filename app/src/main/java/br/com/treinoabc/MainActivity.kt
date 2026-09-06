package br.com.treinoabc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import kotlin.math.max

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var healthClient: HealthConnectClient? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>

    private val providerPackage = HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME
    private val exerciseWritePermission =
        HealthPermission.getWritePermission(ExerciseSessionRecord::class)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            val connected = granted.contains(exerciseWritePermission)
            sendHealthStatus(if (connected) "connected" else "permission_denied")
        }

        setupHealthClient()
        setupWebView()
    }

    private fun setupHealthClient() {
        if (HealthConnectClient.getSdkStatus(this, providerPackage) == HealthConnectClient.SDK_AVAILABLE) {
            healthClient = HealthConnectClient.getOrCreate(this, providerPackage)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
            addJavascriptInterface(AndroidBridge(this@MainActivity), "AndroidHealth")
            loadUrl("file:///android_asset/index.html")
        }
        setContentView(webView)
    }

    inner class AndroidBridge(private val context: Context) {
        @JavascriptInterface
        fun requestHealthConnect() {
            runOnUiThread { requestPermissionOrOpenProvider() }
        }

        @JavascriptInterface
        fun refreshHealthStatus() {
            CoroutineScope(Dispatchers.IO).launch {
                val status = healthStatus()
                withContext(Dispatchers.Main) { sendHealthStatus(status) }
            }
        }

        @JavascriptInterface
        fun syncWorkout(payload: String) {
            CoroutineScope(Dispatchers.IO).launch {
                val result = runCatching { writeWorkoutToHealthConnect(payload) }
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val detail = result.getOrNull() ?: "basic"
                        webView.evaluateJavascript(
                            "window.onHealthSyncResult && window.onHealthSyncResult(true, ${JSONObject.quote(detail)});",
                            null
                        )
                    } else {
                        val msg = result.exceptionOrNull()?.message ?: "Falha ao sincronizar"
                        webView.evaluateJavascript(
                            "window.onHealthSyncResult && window.onHealthSyncResult(false, ${JSONObject.quote(msg)});",
                            null
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissionOrOpenProvider() {
        when (HealthConnectClient.getSdkStatus(this, providerPackage)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                if (healthClient == null) setupHealthClient()
                permissionLauncher.launch(setOf(exerciseWritePermission))
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                val uri = Uri.parse("market://details?id=$providerPackage")
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                    .recoverCatching {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$providerPackage")))
                    }
            }
            else -> sendHealthStatus("unavailable")
        }
    }

    private suspend fun healthStatus(): String {
        val sdkStatus = HealthConnectClient.getSdkStatus(this, providerPackage)
        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) return "update_required"
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) return "unavailable"
        if (healthClient == null) setupHealthClient()
        val granted = healthClient?.permissionController?.getGrantedPermissions().orEmpty()
        return if (granted.contains(exerciseWritePermission)) "connected" else "available"
    }

    private fun sendHealthStatus(status: String) {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript(
            "window.onHealthStatus && window.onHealthStatus(${JSONObject.quote(status)});",
            null
        )
    }

    /**
     * Retorna "detailed" quando a sessão foi gravada com repetições/carga/série.
     * Se o provedor ainda não aceitar os campos novos da 1.2.0-alpha06,
     * faz fallback para a sessão básica e retorna "basic".
     */
    private suspend fun writeWorkoutToHealthConnect(payload: String): String {
        val client = healthClient ?: throw IllegalStateException("Health Connect não disponível")
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.contains(exerciseWritePermission)) {
            throw SecurityException("Permissão do Health Connect não concedida")
        }

        val json = JSONObject(payload)
        val workout = json.optString("workout", "A")
        val sessionId = json.optString("sessionId", "${System.currentTimeMillis()}")
        val startMillis = json.optLong("startMillis", System.currentTimeMillis() - 60_000)
        val endMillis = max(json.optLong("endMillis", System.currentTimeMillis()), startMillis + 1_000)
        val startInstant = Instant.ofEpochMilli(startMillis)
        val endInstant = Instant.ofEpochMilli(endMillis)
        val zone = ZoneId.systemDefault().rules.getOffset(endInstant)

        val metadata = Metadata.manualEntry(
            clientRecordId = "treinoabc-$sessionId",
            clientRecordVersion = 1
        )

        val detailedSegments = buildSegments(json, startInstant, endInstant)
        val title = "Treino $workout — Musculação"
        val notes = buildNotes(json)

        val detailedRecord = ExerciseSessionRecord(
            startTime = startInstant,
            startZoneOffset = zone,
            endTime = endInstant,
            endZoneOffset = zone,
            metadata = metadata,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            title = title,
            notes = notes,
            segments = detailedSegments
        )

        return try {
            client.insertRecords(listOf(detailedRecord))
            "detailed"
        } catch (first: Throwable) {
            val basicRecord = ExerciseSessionRecord(
                startTime = startInstant,
                startZoneOffset = zone,
                endTime = endInstant,
                endZoneOffset = zone,
                metadata = metadata,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
                title = title,
                notes = notes
            )
            client.insertRecords(listOf(basicRecord))
            "basic"
        }
    }

    private fun buildSegments(json: JSONObject, sessionStart: Instant, sessionEnd: Instant): List<ExerciseSegment> {
        val exercises = json.optJSONArray("exercises") ?: return emptyList()
        val result = mutableListOf<ExerciseSegment>()
        var previousEnd = sessionStart

        for (i in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(i)
            val loadKg = exercise.optDouble("loadKg", 0.0).coerceAtLeast(0.0)
            val reps = exercise.optJSONArray("reps")
            val completed = exercise.optJSONArray("completed")
            val completedAt = exercise.optJSONArray("completedAt")

            for (setIndex in 0 until 3) {
                if (completed?.optBoolean(setIndex, false) != true) continue
                val repCount = reps?.optInt(setIndex, 0)?.coerceAtLeast(0) ?: 0
                val rawEnd = completedAt?.optLong(setIndex, 0L) ?: 0L
                var segmentEnd = if (rawEnd > 0) Instant.ofEpochMilli(rawEnd) else previousEnd.plusMillis(500)
                if (segmentEnd.isAfter(sessionEnd)) segmentEnd = sessionEnd

                var segmentStart = segmentEnd.minusMillis(250)
                if (!segmentStart.isAfter(previousEnd)) segmentStart = previousEnd.plusMillis(1)
                if (!segmentEnd.isAfter(segmentStart)) segmentEnd = segmentStart.plusMillis(1)
                if (segmentEnd.isAfter(sessionEnd)) break

                result += ExerciseSegment(
                    startTime = segmentStart,
                    endTime = segmentEnd,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_WEIGHTLIFTING,
                    repetitions = repCount,
                    weight = if (loadKg > 0.0) Mass.kilograms(loadKg) else null,
                    setIndex = setIndex,
                    rateOfPerceivedExertion = null
                )
                previousEnd = segmentEnd
            }
        }
        return result
    }

    private fun buildNotes(json: JSONObject): String {
        val exercises = json.optJSONArray("exercises") ?: return "Treino registrado pelo Treino ABC."
        val lines = mutableListOf<String>()
        for (i in 0 until exercises.length()) {
            val ex = exercises.getJSONObject(i)
            val name = ex.optString("name", "Exercício")
            val load = ex.optDouble("loadKg", 0.0)
            val reps = ex.optJSONArray("reps")
            val completed = ex.optJSONArray("completed")
            val setParts = mutableListOf<String>()
            for (s in 0 until 3) {
                if (completed?.optBoolean(s, false) == true) {
                    val r = reps?.optInt(s, 0) ?: 0
                    setParts += if (r > 0) "S${s+1}: ${r} reps" else "S${s+1}"
                }
            }
            if (setParts.isNotEmpty()) {
                val loadText = if (load > 0) " • ${trimNumber(load)} kg" else ""
                lines += "$name$loadText — ${setParts.joinToString(", ")}"
            }
        }
        return (listOf("Treino registrado pelo Treino ABC.") + lines).joinToString("\n").take(4000)
    }

    private fun trimNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
