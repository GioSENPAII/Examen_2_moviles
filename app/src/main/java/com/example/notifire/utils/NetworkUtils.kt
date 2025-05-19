// Archivo: app/src/main/java/com/example/notifire/utils/NetworkUtils.kt

package com.example.notifire.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object NetworkUtils {

    private val networkLiveData = MutableLiveData<Boolean>()

    fun getNetworkLiveData(context: Context): LiveData<Boolean> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Verificar la conexión actual
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkLiveData.postValue(true)
            }

            override fun onLost(network: Network) {
                networkLiveData.postValue(false)
            }
        }

        // Verificar si hay conexión actualmente
        val networkCapabilities = connectivityManager.activeNetwork ?: return networkLiveData.apply { postValue(false) }
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return networkLiveData.apply { postValue(false) }

        val isConnected = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }

        networkLiveData.postValue(isConnected)

        // Registrar para cambios futuros
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        return networkLiveData
    }
}