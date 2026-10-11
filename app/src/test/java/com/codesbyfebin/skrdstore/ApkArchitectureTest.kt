package com.codesbyfebin.skrdstore
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.zip.ZipOutputStream
import java.util.zip.ZipEntry
class ApkArchitectureTest {
 @Test fun tokenBoundariesAvoidFalseUniversalMatches() {assertFalse(ApkArchitecture.token("install.apk","all"));assertFalse(ApkArchitecture.token("alarm.apk","arm"));assertTrue(ApkArchitecture.token("app-arm64-v8a.apk","arm64-v8a"))}
 @Test fun preferDeviceAbiOverUniversal() {assertTrue(ApkArchitecture.rank("app-arm64.apk",listOf("arm64-v8a"))<ApkArchitecture.rank("app-universal.apk",listOf("arm64-v8a")))}
 @Test fun rejectExplicitForeignArchitecture() {assertEquals(Int.MAX_VALUE,ApkArchitecture.rank("app-x86_64.apk",listOf("arm64-v8a"))); assertEquals(Int.MAX_VALUE,ApkArchitecture.rank("app-x86_64.apk",listOf("x86")))}
 @Test fun inspectRealArchiveRegardlessOfFileName() {
  val f=File.createTempFile("universal-", ".apk")
  try {ZipOutputStream(f.outputStream()).use {it.putNextEntry(ZipEntry("lib/x86_64/libtest.so"));it.write(byteArrayOf(1));it.closeEntry()};val abis=ApkArchitecture.archiveAbis(f);assertFalse(ApkArchitecture.compatible(abis,listOf("arm64-v8a")));assertTrue(ApkArchitecture.compatible(abis,listOf("x86_64")))} finally {f.delete()}
 }
 @Test fun noNativeLibrariesAreAbiIndependent() {assertTrue(ApkArchitecture.compatible(emptySet(),listOf("arm64-v8a")))}
}
