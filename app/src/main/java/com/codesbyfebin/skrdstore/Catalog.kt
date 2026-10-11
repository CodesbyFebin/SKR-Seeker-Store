package com.codesbyfebin.skrdstore

data class DApp(val id: String, val name: String, val category: String, val baseWeight: Double, val contribution: String, val guardian: Boolean = false, val native: Boolean = false, val rating: String = "Demo 4.8", val updated: String = "Demo catalog · 2026-10-11")
object Catalog {
 val apps = listOf(
  DApp("edgeore", "EdgeORE", "DePIN", 1.0, "Offline proof generation", true, true),
  DApp("grass", "Grass", "DePIN", 1.5, "Bandwidth sharing", true),
  DApp("helium", "Helium Mobile", "DePIN", 2.0, "Wireless coverage", true, true),
  DApp("hivemapper", "Hivemapper", "DePIN", 1.8, "Road mapping", true),
  DApp("jupiter", "Jupiter", "DEX", 3.2, "Token swaps", true, true),
  DApp("phantom", "Phantom", "Wallet", 2.5, "Multi-chain wallet", true),
  DApp("backpack", "Backpack", "Wallet", 2.1, "Solana-native wallet", true, true),
  DApp("kamino", "Kamino", "Lending", 1.9, "Auto-optimizing lending"),
  DApp("drift", "Drift", "Perps", 1.7, "Perpetual futures"),
  DApp("lootgo", "LootGo", "GameFi", 1.3, "GPS treasure hunt")
 )
 // MOCK SKR weight: confirmed demo SOL transfers change a local score only.
 fun weight(app: DApp, rows: List<Endorsement>) = app.baseWeight + rows.count { it.appId == app.id && it.status == "CONFIRMED" } * 0.1
}
