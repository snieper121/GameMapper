package com.example.gamemapper.helpers

import android.content.Context
import android.hardware.input.InputManager
import android.view.InputDevice
import android.view.KeyEvent

/**
 * Детектор внешних устройств ввода (клавиатура, мышь, геймпад)
 */
class ExternalDeviceDetector(private val context: Context) {

    private val inputManager: InputManager by lazy {
        context.getSystemService(Context.INPUT_SERVICE) as InputManager
    }

    /**
     * Проверяет, подключена ли внешняя клавиатура
     */
    fun isKeyboardConnected(): Boolean {
        val devices = InputDevice.getDeviceIds()
        return devices.any { deviceId ->
            val device = InputDevice.getDevice(deviceId)
            device.sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD &&
                    device.keyboardType != InputDevice.KEYBOARD_TYPE_VIRTUAL
        }
    }

    /**
     * Проверяет, подключена ли мышь
     */
    fun isMouseConnected(): Boolean {
        val devices = InputDevice.getDeviceIds()
        return devices.any { deviceId ->
            val device = InputDevice.getDevice(deviceId)
            device.sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE
        }
    }

    /**
     * Проверяет, подключен ли геймпад
     */
    fun isGamepadConnected(): Boolean {
        val devices = InputDevice.getDeviceIds()
        return devices.any { deviceId ->
            val device = InputDevice.getDevice(deviceId)
            device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
        }
    }

    /**
     * Получает список подключенных геймпадов
     */
    fun getConnectedGamepads(): List<InputDevice> {
        val devices = InputDevice.getDeviceIds()
        return devices
            .map { InputDevice.getDevice(it) }
            .filter { it.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD }
    }

    /**
     * Получает информацию о подключенных устройствах ввода
     */
    fun getConnectedDevicesInfo(): String {
        val devices = InputDevice.getDeviceIds()
        val info = StringBuilder()

        devices.forEach { deviceId ->
            val device = InputDevice.getDevice(deviceId)
            info.append("Device: ${device.name}\n")
            info.append("  ID: $deviceId\n")
            info.append("  Sources: ${getSourcesDescription(device.sources)}\n")
            if (device.sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {
                info.append("  Keyboard Type: ${getKeyboardTypeDescription(device.keyboardType)}\n")
            }
            info.append("\n")
        }

        return info.toString()
    }

    private fun getSourcesDescription(sources: Int): String {
        val sourceList = mutableListOf<String>()

        if (sources and InputDevice.SOURCE_KEYBOARD == InputDevice.SOURCE_KEYBOARD) {
            sourceList.add("Keyboard")
        }
        if (sources and InputDevice.SOURCE_DPAD == InputDevice.SOURCE_DPAD) {
            sourceList.add("Dpad")
        }
        if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
            sourceList.add("Gamepad")
        }
        if (sources and InputDevice.SOURCE_TOUCHSCREEN == InputDevice.SOURCE_TOUCHSCREEN) {
            sourceList.add("Touchscreen")
        }
        if (sources and InputDevice.SOURCE_MOUSE == InputDevice.SOURCE_MOUSE) {
            sourceList.add("Mouse")
        }
        if (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            sourceList.add("Joystick")
        }

        return sourceList.joinToString(", ")
    }

    private fun getKeyboardTypeDescription(type: Int): String {
        return when (type) {
            InputDevice.KEYBOARD_TYPE_ALPHABETIC -> "Alphabetic"
            InputDevice.KEYBOARD_TYPE_NONE -> "None"
            InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC -> "Non-Alphabetic"
            InputDevice.KEYBOARD_TYPE_VIRTUAL -> "Virtual"
            else -> "Unknown"
        }
    }

    /**
     * Регистрирует слушатель подключения/отключения устройств
     */
    fun registerInputDeviceListener(listener: InputManager.InputDeviceListener) {
        inputManager.registerInputDeviceListener(listener, null)
    }

    /**
     * Отменяет регистрацию слушателя
     */
    fun unregisterInputDeviceListener(listener: InputManager.InputDeviceListener) {
        inputManager.unregisterInputDeviceListener(listener)
    }
}
