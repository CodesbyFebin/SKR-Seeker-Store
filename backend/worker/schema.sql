CREATE TABLE IF NOT EXISTS catalog_apps (
 id TEXT PRIMARY KEY,
 payload_json TEXT NOT NULL CHECK(json_valid(payload_json)),
 updated_at INTEGER NOT NULL
);
-- Public API is read-only. There is no client-submitted endorsement aggregate.
