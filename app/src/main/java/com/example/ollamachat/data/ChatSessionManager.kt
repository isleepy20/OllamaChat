package com.example.ollamachat.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Manages and persists local conversation threads on the device. */
class ChatSessionManager(context: Context) {
    private val file = File(context.filesDir, "chat_sessions.json")

    fun loadSessions(): List<ChatSession> {
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val messagesArray = obj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until messagesArray.length()) {
                    val msgObj = messagesArray.getJSONObject(j)
                    messages.add(
                        ChatMessage(
                            id = msgObj.getLong("id"),
                            role = msgObj.getString("role"),
                            content = msgObj.getString("content"),
                            timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                list.add(
                    ChatSession(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        messages = messages,
                        model = obj.optString("model").takeIf { it.isNotBlank() }
                    )
                )
            }
            list.sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveSessions(sessions: List<ChatSession>) {
        try {
            val array = JSONArray()
            for (s in sessions) {
                val obj = JSONObject()
                obj.put("id", s.id)
                obj.put("title", s.title)
                obj.put("createdAt", s.createdAt)
                obj.put("updatedAt", s.updatedAt)
                obj.put("model", s.model ?: "")
                val msgArray = JSONArray()
                for (m in s.messages) {
                    val mObj = JSONObject()
                    mObj.put("id", m.id)
                    mObj.put("role", m.role)
                    mObj.put("content", m.content)
                    mObj.put("timestamp", m.timestamp)
                    msgArray.put(mObj)
                }
                obj.put("messages", msgArray)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (_: Exception) {}
    }
}
