package com.example.exaoneagent.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences를 사용한 로컬 데이터 저장소
 *
 * 토큰, 사용자 정보 등을 암호화되지 않은 상태로 저장합니다.
 * (프로덕션에서는 EncryptedSharedPreferences 사용 권장)
 */
class PreferenceManager(context: Context) {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ========== Token Management ==========

    fun saveAccessToken(token: String) {
        preferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? {
        return preferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun saveRefreshToken(token: String) {
        preferences.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? {
        return preferences.getString(KEY_REFRESH_TOKEN, null)
    }

    // ========== User Information ==========

    fun saveUserId(userId: Int) {
        preferences.edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): Int {
        return preferences.getInt(KEY_USER_ID, -1)
    }

    fun saveUserEmail(email: String) {
        preferences.edit().putString(KEY_USER_EMAIL, email).apply()
    }

    fun getUserEmail(): String? {
        return preferences.getString(KEY_USER_EMAIL, null)
    }

    fun saveUserName(name: String) {
        preferences.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(): String? {
        return preferences.getString(KEY_USER_NAME, null)
    }

    fun saveDeptName(dept: String) {
        preferences.edit().putString(KEY_DEPT_NAME, dept).apply()
    }

    fun getDeptName(): String? {
        return preferences.getString(KEY_DEPT_NAME, null)
    }

    fun savePosition(position: String) {
        preferences.edit().putString(KEY_POSITION, position).apply()
    }

    fun getPosition(): String? {
        return preferences.getString(KEY_POSITION, null)
    }

    // ========== Login State ==========

    fun saveLoginState(isLoggedIn: Boolean) {
        preferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        return preferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // ========== Clear All ==========

    fun clearAll() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "exaone_prefs"

        // Token keys
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"

        // User info keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_DEPT_NAME = "dept_name"
        private const val KEY_POSITION = "position"

        // Login state
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
}
