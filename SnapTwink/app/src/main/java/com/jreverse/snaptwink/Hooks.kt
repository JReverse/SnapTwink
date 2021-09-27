package com.jreverse.snaptwink

import android.content.Context
import android.widget.Toast
import de.robv.android.xposed.*
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.callbacks.XC_LoadPackage
import de.robv.android.xposed.XposedBridge

import android.os.Environment
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import kotlin.experimental.and


class Hooks : IXposedHookLoadPackage {
    fun copySnapMedia(fromFile: File, homeDir: File): Boolean {
        return try {
            val hexCode = getHex(fromFile)
            val extension: String = when {
                hexCode.contains("ff d8 ff e0 00 10") -> {
                    ".jpg"
                }
                hexCode.contains("89 50 4e 47 0d 0a") -> {
                    ".png"
                }
                checkWildcard(hexCode, "00 00 00 ** 66 74", '*') -> {
                    ".mp4"
                }
                checkWildcard(hexCode, "52 49 46 46 ** **", '*') -> {
                    ".webp"
                }
                else -> {
                    XposedBridge.log("[SnapTwink]: File " + fromFile.name + " is unknown")
                    return false
                }
            }
            XposedBridge.log("[SnapTwink]: File " + fromFile.name + " is a " + extension)
            val inputStream: InputStream = FileInputStream(fromFile)
            val outputStream: OutputStream = FileOutputStream(homeDir.toString() + "/" + fromFile.name + extension)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) {
                outputStream.write(buf, 0, len)
            }
            inputStream.close()
            outputStream.close()
            true
        } catch (e: Exception) {
            XposedBridge.log("[SnapTwink]: ERROR " + e.message)
            false
        }
    }

    private fun getHex(file: File): String {
        val builder = StringBuilder()
        try {
            val fin = FileInputStream(file.path)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (fin.read(buffer).also { bytesRead = it } > -1) for (`in` in 0 until bytesRead) builder.append(String.format("%02x", buffer[`in`] and 0xFF.toByte())).append(if (`in` != bytesRead - 1) " " else "")
        } catch (e: IOException) {
            XposedBridge.log("[SnapTwink]: ERROR " + e.message)
        }
        return builder.substring(0, 17)
    }

    private fun checkWildcard(s1: String, s2: String, wildCard: Char): Boolean {
        if (s1.length != s2.length) return false
        for (i in s1.indices) {
            val c1 = s1[i]
            val c2 = s2[i]
            if (c1 != wildCard && c2 != wildCard && c1 != c2) {
                return false
            }
        }
        return true
    }
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
                        "le8",
                        lpparam.classLoader,
                        "b",
                        "ke8",
                        XC_MethodReplacement.DO_NOTHING
                    ) // Screenshot Bypass
                    val home = File(String.format("%s/SnapTwink/", Environment.getExternalStorageDirectory()))
                    if (!home.exists()) {
                        XposedBridge.log("[SnapTwink]: Home directory not found, creating one...")
                        home.mkdir()
                    }

                    XposedBridge.log("[SnapTwink]: Checking home folder permissions: READ = " + home.canRead().toString() + ", WRITE = " + home.canWrite())
                    if (!home.canRead() || !home.canWrite()) {
                        home.setReadable(true)
                        home.setWritable(true)
                    }

                    val snapStorage = File(String.format("%s/files/file_manager/chat_snap/", snapContext.applicationInfo.dataDir))

                    if (snapStorage.exists()) {
                        XposedBridge.log("[SnapTwink]: Snapchat 'chat_snap' folder exists")
                        XposedBridge.log("[SnapTwink]: Checking folder permissions: READ = " + snapStorage.canRead().toString() + ", WRITE = " + snapStorage.canWrite())
                        if (!snapStorage.canRead()) {
                            XposedBridge.log("[SnapTwink]: Trying to get read access...")
                            if (snapStorage.setReadable(true)) {
                                XposedBridge.log("[SnapTwink]: Read access was successful")
                            } else {
                                XposedBridge.log("[SnapTwink]: Failed to gain read access")
                            }
                        }
                        val files: Array<File> = snapStorage.listFiles()
                        XposedBridge.log("[SnapTwink]: Snapchat 'chat_snap' folder have a length of: " + files.size)
                        for (i in files.indices) {
                            XposedBridge.log("[SnapTwink]: Found file: " + files[i].getPath())
                            if (copySnapMedia(files[i], home)) {
                                XposedBridge.log("[SnapTwink]: Successfully saved Snap: " + files[i].getPath())
                            } else {
                                XposedBridge.log("[SnapTwink]: Failed to save Snap: " + files[i].getPath())
                            }
                        }
                    } else {
                        XposedBridge.log("[SnapTwink]: Snapchat 'chat_snap' folder does NOT exists")
                    }
                }
            })
            XposedBridge.log("[SnapTwink] Hooks completed")
    }
}