package com.opencvapp

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class OpenCVModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "OpenCVModule"
    }

    @ReactMethod
    fun sayHello(promise: Promise) {
        promise.resolve("Hello from OpenCV!")
    }
}