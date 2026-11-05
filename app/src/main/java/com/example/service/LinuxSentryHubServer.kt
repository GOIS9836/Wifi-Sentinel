package com.example.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

object LinuxSentryHubServer {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    const val DEFAULT_PORT = 8721

    fun start(port: Int = DEFAULT_PORT) {
        if (serverJob?.isActive == true && serverSocket != null && !serverSocket!!.isClosed) {
            return
        }

        stop()

        serverJob = scope.launch {
            try {
                val socket = ServerSocket(port)
                serverSocket = socket
                LinuxSentryManager.setServerRunning(true, port)

                while (isActive && !socket.isClosed) {
                    try {
                        val client = socket.accept()
                        scope.launch {
                            handleClient(client)
                        }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            LinuxSentryManager.setServerRunning(false, port, e.message)
                        }
                    }
                }
            } catch (e: Exception) {
                LinuxSentryManager.setServerRunning(false, port, e.message)
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        serverJob?.cancel()
        serverJob = null
        LinuxSentryManager.setServerRunning(false)
    }

    private suspend fun handleClient(client: Socket) = withContext(Dispatchers.IO) {
        try {
            client.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(client.getInputStream(), Charsets.UTF_8))
            val output = client.getOutputStream()

            val requestLine = reader.readLine() ?: return@withContext
            val parts = requestLine.split(" ")
            if (parts.size < 2) return@withContext

            val method = parts[0].uppercase()
            val path = parts[1]

            // Read headers
            var contentLength = 0
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                if (line.lowercase().startsWith("content-length:")) {
                    contentLength = line.substringAfter(":").trim().toIntOrNull() ?: 0
                }
                line = reader.readLine()
            }

            // Read body if POST
            val bodyBuilder = StringBuilder()
            if (contentLength > 0) {
                val buffer = CharArray(1024)
                var bytesRead = 0
                while (bytesRead < contentLength) {
                    val read = reader.read(buffer, 0, minOf(buffer.size, contentLength - bytesRead))
                    if (read == -1) break
                    bodyBuilder.append(buffer, 0, read)
                    bytesRead += read
                }
            }

            when {
                method == "POST" && path.startsWith("/api/v1/telemetry/ingest") -> {
                    val body = bodyBuilder.toString()
                    LinuxSentryManager.ingestTelemetryJson(body)
                    sendJsonResponse(output, 200, """{"status":"BENEDICTUS","accepted":true,"timestamp":${System.currentTimeMillis()}}""")
                }
                method == "GET" && path.startsWith("/api/v1/nodes") -> {
                    val nodes = LinuxSentryManager.linuxNodes.value
                    val sb = StringBuilder("[")
                    nodes.forEachIndexed { i, n ->
                        if (i > 0) sb.append(",")
                        sb.append("""{"hostname":"${n.hostname}","ip":"${n.ipAddress}","distro":"${n.distro}","status":"${n.status.name}","threats":${n.activeThreatsCount}}""")
                    }
                    sb.append("]")
                    sendJsonResponse(output, 200, sb.toString())
                }
                method == "GET" && path.startsWith("/agent/wifi_sentinel_desktop.py") -> {
                    val scriptFile = File("/wifi_sentinel_desktop.py")
                    val content = if (scriptFile.exists() && scriptFile.canRead()) {
                        scriptFile.readText()
                    } else {
                        "# WiFi Sentinel Linux Desktop Agent\nprint('WiFi Sentinel Agent Ready')\n"
                    }
                    sendTextResponse(output, 200, content, "text/x-python")
                }
                method == "GET" && path.startsWith("/agent/install.sh") -> {
                    val installFile = File("/install_linux_sentinel.sh")
                    val content = if (installFile.exists() && installFile.canRead()) {
                        installFile.readText()
                    } else {
                        "#!/usr/bin/env bash\necho 'WiFi Sentinel Installer ready'\n"
                    }
                    sendTextResponse(output, 200, content, "text/x-shellscript")
                }
                else -> {
                    // Tactical Status Landing Page
                    val html = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <title>WiFi Sentinel Hub • Linux Defender</title>
                            <style>
                                body { background: #0B0F19; color: #E2E8F0; font-family: monospace; padding: 24px; }
                                h1 { color: #00E5FF; }
                                .card { background: #131B2E; border: 1px solid #1E293B; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
                                .badge { background: #10B981; color: #0B0F19; font-weight: bold; padding: 3px 8px; border-radius: 4px; }
                                a { color: #00E5FF; }
                            </style>
                        </head>
                        <body>
                            <h1>🛡️ GLOBAL OPIFEX • WIFI SENTINEL APEX HUB (:8721)</h1>
                            <div class="card">
                                <h3>Status: <span class="badge">ACTIVE // BENEDICTUS</span></h3>
                                <p>Ingestion Endpoint: <code>POST /api/v1/telemetry/ingest</code></p>
                                <p>Download Desktop Agent: <a href="/agent/wifi_sentinel_desktop.py">wifi_sentinel_desktop.py</a></p>
                                <p>One-line Linux Deployment: <code>curl -sSL http://THIS_IP:8721/agent/install.sh | bash</code></p>
                            </div>
                        </body>
                        </html>
                    """.trimIndent()
                    sendTextResponse(output, 200, html, "text/html")
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                client.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendJsonResponse(out: OutputStream, code: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(headers.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun sendTextResponse(out: OutputStream, code: Int, text: String, contentType: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $code OK\r\n" +
                "Content-Type: $contentType; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(headers.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }
}
