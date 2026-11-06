package com.ljyh.mei.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.properties.ReadOnlyProperty

// DataStore 单例
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@PublishedApi
@Volatile
internal var cachedPreferencesSnapshot: Preferences? = null

fun updateCachedPreferences(preferences: Preferences?) {
    if (preferences != null) {
        cachedPreferencesSnapshot = preferences
    }
}

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
    cachedPreferencesSnapshot?.get(key) ?: runBlocking(Dispatchers.IO) {
        data.first().also { cachedPreferencesSnapshot = it }[key]
    }

fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>, defaultValue: T): T =
    cachedPreferencesSnapshot?.get(key) ?: runBlocking(Dispatchers.IO) {
        data.first().also { cachedPreferencesSnapshot = it }[key] ?: defaultValue
    }

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initialValue = remember(key) {
        cachedPreferencesSnapshot?.get(key) ?: defaultValue
    }

    val state = remember(key, context) {
        context.dataStore.data
            .map { prefs ->
                cachedPreferencesSnapshot = prefs
                prefs[key] ?: defaultValue
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialValue)

    return remember(state, key, coroutineScope) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
): MutableState<T> {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val initialValue = remember(key) {
        cachedPreferencesSnapshot?.get(key)?.toEnum(defaultValue = defaultValue) ?: defaultValue
    }

    // 监听 Flow
    val state = remember(key, context) {
        context.dataStore.data
            .map { prefs ->
                cachedPreferencesSnapshot = prefs
                prefs[key].toEnum(defaultValue = defaultValue)
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialValue)

    return remember(state, key, coroutineScope) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        context.dataStore.edit {
                            it[key] = value.name
                        }
                    }
                }

            override fun component1() = value
            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

// 辅助扩展函数保持不变
inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
    this?.let { value ->
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }
    } ?: defaultValue

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

fun String.toInetSocketAddress(): InetSocketAddress {
    val (host, port) = split(":")
    return InetSocketAddress.createUnresolved(host, port.toInt())
}
