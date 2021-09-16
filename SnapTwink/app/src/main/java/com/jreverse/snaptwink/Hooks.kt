package com.jreverse.snaptwink

import android.content.Context
import android.widget.Toast
import de.robv.android.xposed.*
import de.robv.android.xposed.XposedHelpers.findAndHookConstructor
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.TimeUnit

class Hooks : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!lpparam.packageName.equals("com.snapchat.android"))
            return

        XposedBridge.log("[SnapTwink] Hooking Snapchat")
        findAndHookMethod(
            "android.app.Application",
            lpparam.classLoader,
            "attach",
            Context::class.java,
            object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: MethodHookParam) {
                    val snapContext = param.args[0] as Context
                    Toast.makeText(snapContext, "SnapTwink Initialized", Toast.LENGTH_SHORT).show()
                    XposedBridge.log("[SnapTwink] Hooked Snapchat")
                    super.afterHookedMethod(param)
                    findAndHookMethod(
                        "QU7",
                        lpparam.classLoader,
                        "b",
                        "PU7",
                        XC_MethodReplacement.DO_NOTHING
                    ) // Screenshot Bypass
                    XposedBridge.log("[SnapTwink] Screenshot Bypassed")
                    /*
                    findAndHookConstructor(
                        "Pqg",
                        lpparam.classLoader,
                        Long::class.java,
                        TimeUnit::class.java,
                        Int::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                super.beforeHookedMethod(param)
                                if(param.args[0].equals(60000)){
                                        XposedHelpers.setLongField(param.thisObject, "c", 2147483647L)
                                        param.args[0] = 2147483647L
                                }
                            }
                        }) // Max Video Length Chat Bypass */
                    /*
                    TODO AD BLOCK
                    findAndHookConstructor(
                        "fe9",
                        lpparam.classLoader,
                        String::class.java,
                        String::class.java,
                        Boolean::class.java,
                        Boolean::class.java,
                        object : XC_MethodHook() {
                            @Throws(Throwable::class)
                            override fun beforeHookedMethod(param: MethodHookParam) {
                                super.beforeHookedMethod(param)
                                if(param.args[0].equals("ADS_HOLDOUT_01")){
                                    if(param.args[2].equals("ADS_IN_AA")) {
                                        XposedHelpers.setBooleanField(param.thisObject, "c", false)
                                        XposedHelpers.setBooleanField(param.thisObject, "d", false)
                                        param.args[3] = false
                                        param.args[4] = false
                                    }
                                }
                            }
                        }
                    ) // Ad Bypass */
                    //XposedBridge.log("[SnapTwink] Ads Bypassed")
                }
            })
            XposedBridge.log("[SnapTwink] Hooks completed")
    }
}