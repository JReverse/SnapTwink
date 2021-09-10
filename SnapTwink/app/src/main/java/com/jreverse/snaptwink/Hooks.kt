package com.jreverse.snaptwink

import android.content.Context
import android.widget.Toast
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Hooks : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (!lpparam.packageName.equals("com.snapchat.android"))
            return;

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
                    Toast.makeText(snapContext, "SnapTwink Initialized", Toast.LENGTH_SHORT).show();
                    XposedBridge.log("[SnapTwink] Hooked Snapchat")
                    super.afterHookedMethod(param)
                    findAndHookMethod(
                        "JS7",
                        lpparam.classLoader,
                        "b",
                        "IS7",
                        XC_MethodReplacement.DO_NOTHING
                    ); // Screenshot Bypass
                    XposedBridge.log("[SnapTwink] Screenshot Bypassed")
                }
            })
    }
}