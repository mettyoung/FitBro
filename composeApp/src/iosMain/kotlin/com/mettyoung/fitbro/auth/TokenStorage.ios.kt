package com.mettyoung.fitbro.auth

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.value
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

actual fun createTokenStorage(): TokenStorage = IosTokenStorage()

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosTokenStorage : TokenStorage {

    override fun saveToken(token: OAuthToken) {
        val json = Json.encodeToString(token)
        val data = (json as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        clearKeychain()
        val attrs = buildQuery {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrService, SERVICE)
            put(kSecAttrAccount, ACCOUNT)
            put(kSecValueData, data)
        }
        @Suppress("UNCHECKED_CAST")
        SecItemAdd(attrs as CFDictionaryRef, null)
    }

    override fun loadToken(): OAuthToken? {
        val query = buildQuery {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrService, SERVICE)
            put(kSecAttrAccount, ACCOUNT)
            put(kSecReturnData, true)
            put(kSecMatchLimit, kSecMatchLimitOne)
        }
        memScoped {
            val resultRef = alloc<CFTypeRefVar>()
            @Suppress("UNCHECKED_CAST")
            val status = SecItemCopyMatching(query as CFDictionaryRef, resultRef.ptr)
            if (status != errSecSuccess) return null
            val nsData = resultRef.value?.rawValue?.let { interpretObjCPointerOrNull<NSData>(it) }
                ?: return null
            val jsonStr = NSString.create(data = nsData, encoding = NSUTF8StringEncoding)
                ?.toString() ?: return null
            return try { Json.decodeFromString<OAuthToken>(jsonStr) } catch (e: Exception) { null }
        }
    }

    override fun clearToken() = clearKeychain()

    private fun clearKeychain() {
        val query = buildQuery {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrService, SERVICE)
            put(kSecAttrAccount, ACCOUNT)
        }
        @Suppress("UNCHECKED_CAST")
        SecItemDelete(query as CFDictionaryRef)
    }

    private fun buildQuery(block: NSMutableDictionary.(cfStr: (CFStringRef?) -> NSString?) -> Unit): NSMutableDictionary {
        val dict = NSMutableDictionary()
        dict.block { ref -> ref?.let { interpretObjCPointerOrNull<NSString>(it.rawValue) } }
        return dict
    }

    private fun NSMutableDictionary.put(key: CFStringRef?, value: Any?) {
        val nsKey = key?.let { interpretObjCPointerOrNull<NSString>(it.rawValue) } ?: return
        val nsValue: Any = when (value) {
            is CPointer<*> -> interpretObjCPointerOrNull<NSString>(value.rawValue) ?: return
            else -> value ?: return
        }
        setObject(nsValue, forKey = nsKey)
    }

    companion object {
        private const val SERVICE = "com.mettyoung.fitbro.oauth"
        private const val ACCOUNT = "cronometer_token"
    }
}
