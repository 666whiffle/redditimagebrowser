package com.example.redditimagebrowser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONObject

/** Reddit installed-app OAuth PKCE flow. No client secret is bundled. */
object RedditAuth {
    private const val REDIRECT = "ribrowser://oauth"
    private const val PREF = "reddit_oauth"
    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    private fun random(): String {
        val b = ByteArray(32); SecureRandom().nextBytes(b)
        return Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
    fun status(c: Context): String = if (prefs(c).getBoolean("connected", false)) "Connected (session established; live feeds are not implemented yet)" else "Not signed in"
    fun logout(c: Context) { prefs(c).edit().clear().apply() }
    fun start(activity: Activity, onStatus: (String) -> Unit) {
        val id = BuildConfig.REDDIT_CLIENT_ID
        if (id.isBlank()) { onStatus("Set REDDIT_CLIENT_ID in app/build.gradle.kts first"); return }
        val verifier = random(); val state = random()
        val challenge = Base64.encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        prefs(activity).edit().putString("state", state).putString("verifier", verifier).apply()
        val url = Uri.parse("https://www.reddit.com/api/v1/authorize.compact").buildUpon()
            .appendQueryParameter("client_id", id)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("state", state)
            .appendQueryParameter("redirect_uri", REDIRECT)
            .appendQueryParameter("duration", "temporary")
            .appendQueryParameter("scope", "identity mysubreddits read")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256").build()
        activity.startActivity(Intent(Intent.ACTION_VIEW, url))
        onStatus("Complete sign-in in your browser")
    }
    fun handleRedirect(activity: Activity, intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme != "ribrowser" || uri.host != "oauth") return
        intent.data = null // Do not exchange the same code twice.
        val p = prefs(activity)
        val state = uri.getQueryParameter("state")
        val code = uri.getQueryParameter("code")
        val verifier = p.getString("verifier", null)
        if (state.isNullOrEmpty() || state != p.getString("state", null) || code.isNullOrEmpty() || verifier.isNullOrEmpty()) {
            p.edit().remove("state").remove("verifier").apply(); return
        }
        p.edit().remove("state").remove("verifier").apply()
        Thread {
            try {
                val conn = URL("https://www.reddit.com/api/v1/access_token").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000; conn.readTimeout = 15000
                conn.doOutput = true
                val basic = Base64.encodeToString("${BuildConfig.REDDIT_CLIENT_ID}:".toByteArray(), Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", "Basic $basic")
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("User-Agent", "android:com.example.redditimagebrowser:0.1.1 (personal prototype)")
                val body = listOf("grant_type" to "authorization_code", "code" to code, "redirect_uri" to REDIRECT, "code_verifier" to verifier)
                    .joinToString("&") { (k,v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
                conn.outputStream.use { it.write(body.toByteArray()) }
                if (conn.responseCode in 200..299) {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    // No token persisted: v0.1.1 verifies the OAuth flow only.
                    if (json.optString("access_token").isNotBlank()) p.edit().putBoolean("connected", true).apply()
                }
                conn.disconnect()
            } catch (_: Exception) { /* User can retry login. */ }
        }.start()
    }
}
