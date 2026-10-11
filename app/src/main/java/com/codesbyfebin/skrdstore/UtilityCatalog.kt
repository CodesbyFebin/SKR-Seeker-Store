package com.codesbyfebin.skrdstore

object UtilityCatalog {
 val apps = listOf(
  "notes" to "Notes",
  "tasks" to "Tasks",
  "calculator" to "Calculator",
  "files" to "Files",
  "music" to "Music",
  "gallery" to "Gallery",
  "device" to "Device Monitor",
  "backup" to "Backup",
  "vault" to "Vault",
  "appmanager" to "App Manager",
  "scanner" to "Scanner",
  "weather" to "Weather",
  "camera" to "Camera",
  "fitness" to "Steps",
  "messaging" to "Nearby Chat",
  "translation" to "Translate",
  "maps" to "Maps",
  "browser" to "Browser"
 )
 val ids = apps.map { it.first }.toSet()
}
