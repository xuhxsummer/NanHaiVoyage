# Cloud save schema

Migration: [`V1__cloud_save.sql`](../../server/src/main/resources/db/migration/V1__cloud_save.sql). All tables use relational columns, foreign keys and primary keys; there are no JSON/blob columns.

| Table | Key / relationship | Stored data |
| --- | --- | --- |
| users | identity `id`; unique username | bcrypt password hash and creation/update timestamps |
| sessions | token_hash; many-to-one users | SHA-256 bearer-token hash, expiry |
| player_profiles | user_id PK/FK; zero-or-one per user | ship, clock, economy, position, capacities, fishing and failure state |
| player_quests | user_id PK/FK; one per saved profile | individual counters, boolean claims, visited-port bitmask |
| player_cargo | (user_id, kind, item_index) | trade, beasts, herbs, fish quantities; costPaid stores per-good cumulative cost |
| player_codex | (user_id, kind, item_index) | beastFound/herbFound booleans, including false entries |
| player_market | (user_id, item_index) | signed price offset; index = port × goods count + good |

Every FK references users with ON DELETE CASCADE. A user without a profile has no saved state (GET returns 204). The first PUT creates profile/quests and all child rows in one transaction. Zero quantities and false discovery flags are stored so array lengths and positions round-trip exactly. Arrays follow `shared/.../Catalog.java`; changing catalog sizes requires a versioned compatibility/migration plan.

Columns use snake_case; DTO fields use camelCase. `StateStore` maps the shared DTO fields to the explicit columns listed below. Adding a scalar requires a SQL migration; adding an array additionally requires a mapping and validation rule. Tests round-trip every field so a schema/DTO mismatch fails visibly.

## player_profiles columns

| Column | SQL type |
| --- | --- |
| `user_id` | BIGINT |
| `x` | REAL |
| `y` | REAL |
| `heading_deg` | REAL |
| `docked_port` | INTEGER |
| `failed` | BOOLEAN |
| `hull` | REAL |
| `hull_max` | REAL |
| `supply` | REAL |
| `supply_max` | REAL |
| `silver` | INTEGER |
| `debt` | INTEGER |
| `cargo_cap` | INTEGER |
| `warehouse_level` | INTEGER |
| `cannon_level` | INTEGER |
| `cannon_damage` | INTEGER |
| `crew` | INTEGER |
| `crew_cap` | INTEGER |
| `crew_cap_level` | INTEGER |
| `last_port` | INTEGER |
| `ship` | INTEGER |
| `ship_owned` | INTEGER |
| `game_day` | INTEGER |
| `day_min` | REAL |
| `fishers` | INTEGER |
| `fisher_cap_level` | INTEGER |
| `fish_tool_level` | INTEGER |
| `fish_skill_level` | INTEGER |
| `fishing_on` | BOOLEAN |
| `fish_caught_total` | INTEGER |
| `updated_at` | TIMESTAMP WITH TIME ZONE |

## player_quests columns

| Column | SQL type |
| --- | --- |
| `user_id` | BIGINT |
| `quest_visit_port_set` | INTEGER |
| `quest_sell_silk` | INTEGER |
| `quest_visit_ports` | INTEGER |
| `quest_defeated_pirates` | INTEGER |
| `quest_beasts_found` | INTEGER |
| `quest_debt_paid` | BOOLEAN |
| `quest_silver_peak` | INTEGER |
| `quest_warehouse_ups` | INTEGER |
| `quest_hired_crew` | INTEGER |
| `quest_island_visits` | INTEGER |
| `quest_refill_count` | INTEGER |
| `quest_repair_count` | INTEGER |
| `quest_buy_count` | INTEGER |
| `quest_profitable_sell` | BOOLEAN |
| `quest_intel_viewed` | BOOLEAN |
| `quest_upgrade_count` | INTEGER |
| `quest_buy_tea` | INTEGER |
| `quest_sell_porcelain` | INTEGER |
| `quest_claim_island_visit` | BOOLEAN |
| `quest_claim_refill` | BOOLEAN |
| `quest_claim_repair` | BOOLEAN |
| `quest_claim_buy` | BOOLEAN |
| `quest_claim_profitable_sell` | BOOLEAN |
| `quest_claim_win_combat` | BOOLEAN |
| `quest_claim_intel_viewed` | BOOLEAN |
| `quest_claim_upgrade_any` | BOOLEAN |
| `quest_claim_buy_tea` | BOOLEAN |
| `quest_claim_sell_porcelain` | BOOLEAN |
| `quest_claim_island_explore` | BOOLEAN |
| `quest_claim_sell_silk` | BOOLEAN |
| `quest_claim_visit_ports` | BOOLEAN |
| `quest_claim_defeated_pirates` | BOOLEAN |
| `quest_claim_beasts_found` | BOOLEAN |
| `quest_claim_debt_paid` | BOOLEAN |
| `quest_claim_silver_peak` | BOOLEAN |
| `quest_claim_warehouse_ups` | BOOLEAN |
| `quest_claim_hired_crew` | BOOLEAN |
| `updated_at` | TIMESTAMP WITH TIME ZONE |

## Atomicity and conflicts

GET and PUT lock the user's row. PUT validates the full state, replaces its normalized rows atomically, and assigns the same server-generated UTC `updated_at` to profile and quests. Timestamps are generated after acquiring the lock. A failed transaction retains the prior complete state. Users' creation/update timestamps currently describe account creation; saves update profile/quest timestamps.

The last successfully applied PUT wins. A delayed offline upload can overwrite newer progress from another device; timestamps are informational, not a conditional-write mechanism. Session ownership prevents one account from accessing another account's rows. Player inventory values remain client-authored; this v1 is not an authoritative gameplay server.
