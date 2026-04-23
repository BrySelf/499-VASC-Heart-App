package com.opencvapp

import android.app.Application
import android.util.Log
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.mrousavy.camera.frameprocessors.FrameProcessorPluginRegistry
import org.opencv.android.OpenCVLoader

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost =
    object : DefaultReactNativeHost(this) {
      override fun getPackages(): List<ReactPackage> =
        PackageList(this).packages.apply {
          // This is what connects your OpenCVModule.kt to JavaScript
          add(OpenCVPackage())
        }

      override fun getJSMainModuleName(): String = "index"
      override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG
      override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
    }

  override fun onCreate() {
    super.onCreate()

    // 1. Load OpenCV
    if (OpenCVLoader.initDebug()) {
      Log.i("OpenCV", "OpenCV successfully loaded")
    }

    // 2. Register the Plugin
    // Even without the JS hook, this tells the Camera to keep this plugin
    // in its internal registry for the native session.
    FrameProcessorPluginRegistry.addFrameProcessorPlugin("rppg") { proxy, options ->
      RPPGFrameProcessorPlugin(proxy, options)
    }

    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      load()
    }
  }
}