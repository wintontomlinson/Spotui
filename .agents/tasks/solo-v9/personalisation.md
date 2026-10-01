# Fix #5 — Personalised Home through TasteRanker (what changed)

Goal: every Home shelf must be ranked through the EXISTING on-device `TasteRanker` from the
`TasteProfile`, not a generic global list. Must work login-free (local history) and with login.
Must preserve `recordOutcome` and the TasteProfile JSON schema exactly.

## State before this change

In `FreeHomeViewModel.kt`, these shelves already routed through
`com.music.spotui.data.recommendation.TasteRanker.rank(...)`:

- **Mix for you** (`buildTasteBlocks`) — ranked, seed=null, recent=history, limit=25.
- **Because you liked X** (`buildBecauseYouLiked`) — ranked, seed=liked track, recent=history, limit=20.
- **Trending now** (`fetchTrending`) — ranked, seed=null, recent=history, limit=8, with raw-order fallback.

The gap: the generic/curated shelves (the personalised `buildSections` rows — "More like <artist>",
"Because you played <song>", the time shelf, the mood shelf, "Fresh for you" — plus the rotated
curated evergreen rows) were all filled by `fetchRow(index, query)`, which rendered the raw
`YouTube.search(query)` global relevance order **unranked**. So those rows were not personalised by
the TasteProfile.

## Change made

`fetchRow(index, query)` now:

1. Pulls a wider candidate pool: `searchSongs(query, limit = 24)` (was 12) so the ranker has room to
   reorder and still spend an exploration slot on fresh content.
2. Ranks the candidates through the existing engine:
   `TasteRanker.rank(context, candidates, seed = null, recent = getListeningHistory(context), limit = 12)`
   on `Dispatchers.Default`, wrapped in `runCatching`.
3. Falls back to the raw search order (`candidates.take(12)`) when ranking returns empty, mirroring
   the existing `fetchTrending()` fallback, so a shelf is never blanked.

Result: EVERY Home shelf path (`fetchRow`, mix, becauseYouLiked, trending) now runs through
`TasteRanker`, so Home reflects the user's genre/mood/artist affinity, time-of-day and recency.

## Constraints preserved

- **Read-only** use of the profile: `fetchRow` only calls `getListeningHistory` and
  `TasteRanker.rank`. `recordOutcome` is NOT called or modified anywhere in this change.
- The **TasteProfile JSON schema** is untouched (no new fields, no serialization changes).
- Works **login-free** (local listening history is the `recent` signal) and **with login** (history
  is still local). On a fresh install with no history, `TasteRanker` ranks on its built-in
  signals/exploration and the raw-order fallback guarantees content.
- On-device and lightweight: one extra in-memory rank pass per shelf on a background dispatcher; no
  new network calls beyond the existing search.
