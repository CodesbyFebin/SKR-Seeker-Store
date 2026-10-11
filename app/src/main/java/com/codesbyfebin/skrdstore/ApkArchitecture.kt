package com.codesbyfebin.skrdstore

import java.util.zip.ZipFile
import java.io.File
/** ABI naming/token rules adapted from RepoStore ApkArchitectureHelper (MIT, Samyak Kamble).
 * Actual archive lib entries, rather than filename heuristics, determine install compatibility.
 */
object ApkArchitecture {
 private val patterns=mapOf(
  "arm64-v8a" to listOf("arm64-v8a","arm64","aarch64","arm64_v8a"),
  "armeabi-v7a" to listOf("armeabi-v7a","armeabi_v7a","armv7","arm-v7a","armeabi"),
  "x86_64" to listOf("x86_64","x86-64","x64","amd64"),
  "x86" to listOf("x86","x32","i686","i386"))
 fun token(name: String, token: String): Boolean {
  val n=name.lowercase(); val t=token.lowercase(); if(t.isEmpty()) return false
  var i=n.indexOf(t)
  while(i>=0) {
   if((i==0 || !n[i-1].isLetterOrDigit()) && (i+t.length==n.length || !n[i+t.length].isLetterOrDigit())) return true
   i=n.indexOf(t,i+1)
  }
  return false
 }
 fun rank(name: String, supported: List<String>): Int {
  if(patterns["x86_64"].orEmpty().any {token(name,it)} && "x86_64" !in supported) return Int.MAX_VALUE
  supported.forEachIndexed { index,abi -> if(patterns[abi].orEmpty().any {token(name,it)}) return index }
  if(listOf("universal","all","fat","noarch").any {token(name,it)}) return supported.size
  if(patterns.values.flatten().any {token(name,it)}) return Int.MAX_VALUE
  return supported.size+1 // unknown filename is inspected after download, never declared compatible
 }
 fun archiveAbis(file: File): Set<String> = ZipFile(file).use { z ->
  z.entries().asSequence().map {it.name}.filter {it.startsWith("lib/") && it.endsWith(".so")}.map {it.split('/')[1]}.toSet()
 }
 fun compatible(archiveAbis: Set<String>, supported: List<String>)=archiveAbis.isEmpty() || archiveAbis.any {it in supported}
}
