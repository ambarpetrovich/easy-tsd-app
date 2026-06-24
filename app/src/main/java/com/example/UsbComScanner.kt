package com.example

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executors

class UsbComScanner(private val context: Context) {
    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null
    private val lineBuffer = ByteArrayOutputStream()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _barcodeFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val barcodeFlow: SharedFlow<String> = _barcodeFlow.asSharedFlow()
    
    private val _statusFlow = MutableSharedFlow<String>(extraBufferCapacity = 5)
    val statusFlow: SharedFlow<String> = _statusFlow.asSharedFlow()

    private var reconnectVendorId = -1
    private var reconnectProductId = -1
    private var isReading = false
    private var reconnectEnabled = false
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null

    companion object {
        const val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"
        const val MAX_RECONNECT_ATTEMPTS = 5
        const val RECONNECT_DELAY_MS = 2000L
        const val TAG = "UsbComScanner"
    }

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_USB_PERMISSION) {
                synchronized(this) {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false) && device != null) {
                        connectToDevice(device)
                    } else {
                        sendStatus("Разрешение отклонено")
                    }
                }
            }
        }
    }

    private val detachReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                device?.let {
                    if (it.vendorId == reconnectVendorId && it.productId == reconnectProductId) {
                        Log.i(TAG, "Device detached")
                        sendStatus("Устройство отключено")
                        cleanup()
                        scheduleReconnect()
                    }
                }
            } else if (intent.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                }
                device?.let {
                    if (it.vendorId == reconnectVendorId && it.productId == reconnectProductId) {
                        Log.i(TAG, "Device attached, attempting connection")
                        autoConnect(it.vendorId, it.productId)
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(permissionReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(permissionReceiver, filter)
        }

        val detachFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(detachReceiver, detachFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(detachReceiver, detachFilter)
        }
    }

    private fun sendStatus(status: String) {
        scope.launch { _statusFlow.emit(status) }
    }

    fun autoConnect(vendorId: Int = -1, productId: Int = -1) {
        reconnectVendorId = vendorId
        reconnectProductId = productId
        reconnectEnabled = true
        reconnectAttempts = 0

        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val driver = if (vendorId != -1 && productId != -1) {
            drivers.find { it.device.vendorId == vendorId && it.device.productId == productId }
        } else {
            drivers.firstOrNull()
        }

        if (driver != null) {
            val device = driver.device
            reconnectVendorId = device.vendorId
            reconnectProductId = device.productId

            if (usbManager.hasPermission(device)) {
                connectToDevice(device)
            } else {
                sendStatus("Ожидание разрешения...")
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val intent = Intent(ACTION_USB_PERMISSION).apply { setPackage(context.packageName) }
                val pi = PendingIntent.getBroadcast(context, 0, intent, flags)
                usbManager.requestPermission(device, pi)
            }
        } else {
            sendStatus("Устройство не найдено")
        }
    }

    private fun connectToDevice(device: UsbDevice) {
        val driver = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager).find { it.device == device }
        if (driver == null) {
            sendStatus("Драйвер не найден")
            return
        }

        try {
            val connection = usbManager.openDevice(device)
            if (connection == null) {
                sendStatus("Не удалось открыть устройство")
                scheduleReconnect()
                return
            }

            val port = driver.ports[0]
            port.open(connection)
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port.dtr = true
            port.rts = true
            serialPort = port

            reconnectAttempts = 0
            sendStatus("Подключено")
            startReading()
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            sendStatus("Ошибка подключения")
            scheduleReconnect()
        }
    }

    private fun startReading() {
        if (serialPort == null) return
        isReading = true
        lineBuffer.reset()
        
        ioManager = SerialInputOutputManager(serialPort, object : SerialInputOutputManager.Listener {
            override fun onNewData(data: ByteArray) {
                var foundDelimiter = false
                synchronized(lineBuffer) {
                    for (b in data) {
                        if (b == 10.toByte() || b == 13.toByte()) {
                            foundDelimiter = true
                        } else {
                            lineBuffer.write(b.toInt())
                        }
                    }
                }
                if (foundDelimiter) {
                    flushBuffer()
                } else {
                    scope.launch {
                        delay(100)
                        flushBuffer()
                    }
                }
            }

            override fun onRunError(e: Exception) {
                Log.e(TAG, "Run error", e)
                if (isReading) {
                    sendStatus("Связь потеряна")
                    cleanup()
                    scheduleReconnect()
                }
            }
        })
        Executors.newSingleThreadExecutor().submit(ioManager)
    }

    private fun flushBuffer() {
        val bytes: ByteArray
        synchronized(lineBuffer) {
            bytes = lineBuffer.toByteArray()
            lineBuffer.reset()
        }
        if (bytes.isNotEmpty()) {
            val raw = String(bytes, Charsets.UTF_8).trim()
            val barcode = if (raw.contains("\uFFFD")) {
                String(bytes, java.nio.charset.Charset.forName("CP1251")).trim()
            } else raw
            
            if (barcode.isNotEmpty()) {
                scope.launch { _barcodeFlow.emit(barcode) }
            }
        }
    }

    private fun scheduleReconnect() {
        if (!reconnectEnabled || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            sendStatus(if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) "Переподключение не удалось" else "Отключено")
            return
        }
        reconnectAttempts++
        sendStatus("Переподключение ($reconnectAttempts/$MAX_RECONNECT_ATTEMPTS)...")
        
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(RECONNECT_DELAY_MS)
            autoConnect(reconnectVendorId, reconnectProductId)
        }
    }

    private fun cleanup() {
        isReading = false
        ioManager?.stop()
        ioManager = null
        try { serialPort?.close() } catch (e: Exception) {}
        serialPort = null
    }

    fun disconnect() {
        reconnectEnabled = false
        reconnectJob?.cancel()
        cleanup()
        sendStatus("Отключено")
    }

    fun destroy() {
        disconnect()
        try { context.unregisterReceiver(permissionReceiver) } catch (e: Exception) {}
        try { context.unregisterReceiver(detachReceiver) } catch (e: Exception) {}
        scope.cancel()
    }
}
