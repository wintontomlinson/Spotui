/* =========================================================================
 * Spotu Web — a YouTube-powered music player (vanilla JS, no build step).
 *
 * Playback uses the official YouTube IFrame Player API (audio from a hidden
 * player). Search uses the YouTube Data API v3 when a key is provided in
 * Settings; without a key, the curated featured playlists still play fully.
 * ========================================================================= */

(() => {
  "use strict";

  // ---- DOM helpers -------------------------------------------------------
  const $ = (id) => document.getElementById(id);
  const el = (tag, cls, html) => {
    const n = document.createElement(tag);
    if (cls) n.className = cls;
    if (html != null) n.innerHTML = html;
    return n;
  };
  const escapeHtml = (s) =>
    String(s).replace(/[&<>"']/g, (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])
    );
  const fmtTime = (sec) => {
    sec = Math.max(0, Math.floor(sec || 0));
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${m}:${s.toString().padStart(2, "0")}`;
  };
  const thumb = (videoId) => `https://i.ytimg.com/vi/${videoId}/mqdefault.jpg`;

  // ---- State -------------------------------------------------------------
  const KEY_STORE = "spotu_web_yt_key";
  const state = {
    queue: [],          // array of track objects {title, artist, videoId, duration?}
    index: -1,          // current index into queue
    playing: false,
    shuffle: false,
    repeat: "off",      // "off" | "all" | "one"
    apiKey: localStorage.getItem(KEY_STORE) || "",
    seeking: false,
  };

  let player = null;    // YT.Player instance
  let ytReady = false;
  let pendingPlay = null; // track queued before the API finished loading
  let progressTimer = null;

  // =========================================================================
  // YouTube IFrame API
  // =========================================================================
  window.onYouTubeIframeAPIReady = function () {
    player = new YT.Player("yt-host", {
      height: "1",
      width: "1",
      playerVars: { autoplay: 0, controls: 0, disablekb: 1, playsinline: 1 },
      events: {
        onReady: () => {
          ytReady = true;
          player.setVolume(Number($("volume").value));
          if (pendingPlay) {
            const p = pendingPlay;
            pendingPlay = null;
            loadVideo(p);
          }
        },
        onStateChange: onPlayerState,
        onError: onPlayerError,
      },
    });
  };

  function loadYouTubeApi() {
    const tag = document.createElement("script");
    tag.src = "https://www.youtube.com/iframe_api";
    document.head.appendChild(tag);
  }

  function onPlayerState(e) {
    const S = YT.PlayerState;
    if (e.data === S.PLAYING) {
      setPlayingUI(true);
      startProgress();
    } else if (e.data === S.PAUSED) {
      setPlayingUI(false);
      stopProgress();
    } else if (e.data === S.ENDED) {
      stopProgress();
      handleTrackEnd();
    } else if (e.data === S.BUFFERING) {
      // update duration once known
      updateDuration();
    }
  }

  function onPlayerError() {
    // Video unavailable / embedding disabled — skip to the next track.
    toast("Can't play that track, skipping…");
    setTimeout(() => next(), 600);
  }

  // =========================================================================
  // Playback core
  // =========================================================================
  function playTrack(track, queue, startIndex) {
    if (queue) {
      state.queue = queue.slice();
      state.index = startIndex != null ? startIndex : 0;
    }
    if (!ytReady || !player) {
      pendingPlay = track;
      renderNowPlaying(track);
      return;
    }
    loadVideo(track);
  }

  function loadVideo(track) {
    renderNowPlaying(track);
    player.loadVideoById(track.videoId);
    player.playVideo();
    state.playing = true;
    refreshTrackHighlights();
    renderQueue();
  }

  function togglePlay() {
    if (!ytReady || state.index < 0) {
      // Nothing loaded yet — start the first thing available.
      if (state.queue.length) playTrack(state.queue[0], state.queue, 0);
      return;
    }
    const st = player.getPlayerState();
    if (st === YT.PlayerState.PLAYING) {
      player.pauseVideo();
    } else {
      player.playVideo();
    }
  }

  function next(auto) {
    if (!state.queue.length) return;
    if (state.repeat === "one" && auto) {
      loadVideo(state.queue[state.index]);
      return;
    }
    let i;
    if (state.shuffle) {
      i = randomOtherIndex();
    } else {
      i = state.index + 1;
      if (i >= state.queue.length) {
        if (state.repeat === "all" || !auto) i = 0;
        else { setPlayingUI(false); return; }
      }
    }
    state.index = i;
    loadVideo(state.queue[i]);
  }

  function prev() {
    if (!state.queue.length) return;
    // If more than 3s in, restart the current track first (like most players).
    if (ytReady && player.getCurrentTime && player.getCurrentTime() > 3) {
      player.seekTo(0, true);
      return;
    }
    let i = state.shuffle ? randomOtherIndex() : state.index - 1;
    if (i < 0) i = state.repeat === "all" ? state.queue.length - 1 : 0;
    state.index = i;
    loadVideo(state.queue[i]);
  }

  function handleTrackEnd() { next(true); }

  function randomOtherIndex() {
    if (state.queue.length <= 1) return state.index;
    let i;
    do { i = Math.floor(Math.random() * state.queue.length); } while (i === state.index);
    return i;
  }

  // =========================================================================
  // Progress / seek / volume
  // =========================================================================
  function startProgress() {
    stopProgress();
    progressTimer = setInterval(tickProgress, 500);
  }
  function stopProgress() {
    if (progressTimer) { clearInterval(progressTimer); progressTimer = null; }
  }
  function updateDuration() {
    if (!ytReady || !player.getDuration) return;
    const d = player.getDuration();
    if (d > 0) $("durTime").textContent = fmtTime(d);
  }
  function tickProgress() {
    if (!ytReady || state.seeking || !player.getDuration) return;
    const d = player.getDuration();
    const t = player.getCurrentTime();
    if (d > 0) {
      const pct = (t / d) * 1000;
      const seek = $("seek");
      seek.value = pct;
      setFill(seek, (t / d) * 100);
      $("curTime").textContent = fmtTime(t);
      $("durTime").textContent = fmtTime(d);
    }
  }
  function setFill(rangeEl, pct) {
    rangeEl.classList.add("filled");
    rangeEl.style.setProperty("--fill", `${Math.max(0, Math.min(100, pct))}%`);
  }

  // =========================================================================
  // Rendering
  // =========================================================================
  function renderNowPlaying(track) {
    $("nowArt").src = thumb(track.videoId);
    $("nowTitle").textContent = track.title;
    $("nowArtist").textContent = track.artist || "";
    document.title = `${track.title} · Spotu Web`;
  }

  function setPlayingUI(playing) {
    state.playing = playing;
    $("iconPlay").hidden = playing;
    $("iconPause").hidden = !playing;
  }

  function trackRow(track, i, opts = {}) {
    const row = el("div", "track");
    const isCurrent =
      state.index >= 0 &&
      state.queue[state.index] &&
      state.queue[state.index].videoId === track.videoId;
    if (isCurrent) row.classList.add("playing");

    row.innerHTML = `
      ${opts.showIndex !== false ? `<div class="track-idx">${i + 1}</div>` : ""}
      <img class="track-art" src="${thumb(track.videoId)}" alt="" loading="lazy" />
      <div class="track-info">
        <div class="track-title">${escapeHtml(track.title)}</div>
        <div class="track-artist">${escapeHtml(track.artist || "")}</div>
      </div>
      <div class="track-dur">${track.duration ? fmtTime(track.duration) : ""}</div>`;
    row.addEventListener("click", opts.onClick);
    return row;
  }

  function refreshTrackHighlights() {
    document.querySelectorAll(".track").forEach((r) => r.classList.remove("playing"));
    // Re-render lists that show the current track state.
    const active = document.querySelector(".nav-item.active")?.dataset.view;
    if (active === "queue") renderQueue();
  }

  // ---- Home / playlists --------------------------------------------------
  function renderPlaylists() {
    const grid = $("playlistGrid");
    grid.innerHTML = "";
    (window.FEATURED_PLAYLISTS || []).forEach((pl) => {
      const cover = pl.tracks[0] ? thumb(pl.tracks[0].videoId) : "";
      const card = el("button", "card");
      card.innerHTML = `
        <img class="card-art" src="${cover}" alt="" loading="lazy" />
        <div class="card-title">${escapeHtml(pl.title)}</div>
        <div class="card-sub">${escapeHtml(pl.description || "")}</div>`;
      card.addEventListener("click", () => openPlaylist(pl));
      grid.appendChild(card);
    });
  }

  function openPlaylist(pl) {
    const title = $("tracksTitle");
    title.hidden = false;
    title.textContent = pl.title;
    const list = $("playlistTracks");
    list.innerHTML = "";
    pl.tracks.forEach((t, i) => {
      list.appendChild(
        trackRow(t, i, { onClick: () => playTrack(t, pl.tracks, i) })
      );
    });
    list.scrollIntoView({ behavior: "smooth", block: "start" });
  }

  // ---- Search ------------------------------------------------------------
  let searchDebounce = null;
  function onSearchInput(value) {
    $("searchClear").hidden = !value;
    clearTimeout(searchDebounce);
    if (!value.trim()) {
      $("searchResults").innerHTML = "";
      $("searchEmpty").hidden = false;
      return;
    }
    searchDebounce = setTimeout(() => runSearch(value.trim()), 400);
  }

  async function runSearch(query) {
    switchView("search");
    const results = $("searchResults");
    const empty = $("searchEmpty");
    empty.hidden = true;

    if (!state.apiKey) {
      // No key: search within the curated catalog so the box still does something.
      const local = searchLocal(query);
      if (local.length) {
        renderSearchResults(local, `Results for "${query}" (offline catalog)`);
      } else {
        results.innerHTML = "";
        empty.hidden = false;
        empty.innerHTML = `<div class="empty-icon">🔑</div>
          <p>Add a free YouTube API key in <b>Settings</b> to search all of YouTube.<br>
          No match for "${escapeHtml(query)}" in the offline catalog.</p>`;
      }
      return;
    }

    results.innerHTML = `<div class="loading"><div class="spinner"></div>Searching…</div>`;
    try {
      const items = await ytSearch(query, state.apiKey);
      if (!items.length) {
        results.innerHTML = "";
        empty.hidden = false;
        empty.innerHTML = `<div class="empty-icon">🔎</div><p>No results for "${escapeHtml(query)}".</p>`;
        return;
      }
      renderSearchResults(items, `Results for "${query}"`);
    } catch (err) {
      results.innerHTML = "";
      empty.hidden = false;
      empty.innerHTML = `<div class="empty-icon">⚠️</div>
        <p>Search failed: ${escapeHtml(err.message)}.<br>Check your API key in Settings.</p>`;
    }
  }

  function renderSearchResults(items, titleText) {
    $("searchTitle").textContent = titleText || "Search";
    const results = $("searchResults");
    results.innerHTML = "";
    items.forEach((t, i) => {
      results.appendChild(
        trackRow(t, i, { showIndex: false, onClick: () => playTrack(t, items, i) })
      );
    });
  }

  function searchLocal(query) {
    const q = query.toLowerCase();
    const seen = new Set();
    const out = [];
    (window.FEATURED_PLAYLISTS || []).forEach((pl) =>
      pl.tracks.forEach((t) => {
        const hay = `${t.title} ${t.artist}`.toLowerCase();
        if (hay.includes(q) && !seen.has(t.videoId)) {
          seen.add(t.videoId);
          out.push(t);
        }
      })
    );
    return out;
  }

  async function ytSearch(query, key) {
    const url =
      "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video" +
      "&videoCategoryId=10&maxResults=20&q=" +
      encodeURIComponent(query) +
      "&key=" +
      encodeURIComponent(key);
    const res = await fetch(url);
    if (!res.ok) {
      let msg = `HTTP ${res.status}`;
      try {
        const j = await res.json();
        msg = j.error?.message || msg;
      } catch (_) {}
      throw new Error(msg);
    }
    const data = await res.json();
    return (data.items || [])
      .filter((it) => it.id && it.id.videoId)
      .map((it) => ({
        title: decodeEntities(it.snippet.title),
        artist: decodeEntities(it.snippet.channelTitle || ""),
        videoId: it.id.videoId,
      }));
  }

  function decodeEntities(s) {
    const t = document.createElement("textarea");
    t.innerHTML = s;
    return t.value;
  }

  // ---- Queue view --------------------------------------------------------
  function renderQueue() {
    const list = $("queueList");
    const empty = $("queueEmpty");
    if (!state.queue.length) {
      list.innerHTML = "";
      empty.hidden = false;
      return;
    }
    empty.hidden = true;
    list.innerHTML = "";
    state.queue.forEach((t, i) => {
      list.appendChild(
        trackRow(t, i, {
          onClick: () => { state.index = i; loadVideo(t); },
        })
      );
    });
  }

  // =========================================================================
  // View routing
  // =========================================================================
  function switchView(view) {
    document.querySelectorAll(".nav-item").forEach((b) =>
      b.classList.toggle("active", b.dataset.view === view)
    );
    ["home", "search", "queue"].forEach((v) => {
      $(`view-${v}`).hidden = v !== view;
    });
    if (view === "queue") renderQueue();
  }

  // =========================================================================
  // Toast
  // =========================================================================
  let toastTimer = null;
  function toast(msg) {
    let t = $("toast");
    if (!t) {
      t = el("div");
      t.id = "toast";
      Object.assign(t.style, {
        position: "fixed", bottom: "104px", left: "50%", transform: "translateX(-50%)",
        background: "#000000e6", color: "#fff", padding: "10px 18px", borderRadius: "24px",
        fontSize: "14px", zIndex: 50, border: "1px solid #2a2a33", pointerEvents: "none",
      });
      document.body.appendChild(t);
    }
    t.textContent = msg;
    t.style.opacity = "1";
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => (t.style.opacity = "0"), 2200);
  }

  // =========================================================================
  // Wiring
  // =========================================================================
  function updateKeyHint() {
    $("keyHint").textContent = state.apiKey
      ? "✅ Search enabled (API key set)."
      : "Tip: add a YouTube API key in Settings to search all of YouTube.";
  }

  function bind() {
    // Transport
    $("playBtn").addEventListener("click", togglePlay);
    $("nextBtn").addEventListener("click", () => next(false));
    $("prevBtn").addEventListener("click", prev);
    $("shuffleBtn").addEventListener("click", () => {
      state.shuffle = !state.shuffle;
      $("shuffleBtn").classList.toggle("active", state.shuffle);
      toast(state.shuffle ? "Shuffle on" : "Shuffle off");
    });
    $("repeatBtn").addEventListener("click", () => {
      state.repeat = state.repeat === "off" ? "all" : state.repeat === "all" ? "one" : "off";
      $("repeatBtn").classList.toggle("active", state.repeat !== "off");
      $("repeatBtn").title = "Repeat: " + state.repeat;
      toast("Repeat: " + state.repeat);
    });

    // Seek
    const seek = $("seek");
    seek.addEventListener("input", () => {
      state.seeking = true;
      setFill(seek, seek.value / 10);
    });
    seek.addEventListener("change", () => {
      if (ytReady && player.getDuration) {
        const d = player.getDuration();
        player.seekTo((seek.value / 1000) * d, true);
      }
      state.seeking = false;
    });

    // Volume
    const vol = $("volume");
    setFill(vol, vol.value);
    vol.addEventListener("input", () => {
      const v = Number(vol.value);
      setFill(vol, v);
      if (ytReady) player.setVolume(v);
      $("iconVol").style.opacity = v === 0 ? 0.4 : 1;
    });
    $("volBtn").addEventListener("click", () => {
      if (!ytReady) return;
      if (player.isMuted()) { player.unMute(); vol.value = player.getVolume() || 60; }
      else { player.mute(); }
      const v = player.isMuted() ? 0 : Number(vol.value);
      setFill(vol, v);
    });

    // Nav
    document.querySelectorAll(".nav-item").forEach((b) =>
      b.addEventListener("click", () => switchView(b.dataset.view))
    );

    // Search
    $("searchInput").addEventListener("input", (e) => onSearchInput(e.target.value));
    $("searchInput").addEventListener("focus", () => switchView("search"));
    $("searchClear").addEventListener("click", () => {
      $("searchInput").value = "";
      onSearchInput("");
      $("searchInput").focus();
    });

    // Settings
    $("openSettings").addEventListener("click", () => {
      $("apiKeyInput").value = state.apiKey;
      $("settingsDlg").showModal();
    });
    $("saveKey").addEventListener("click", (e) => {
      e.preventDefault();
      state.apiKey = $("apiKeyInput").value.trim();
      if (state.apiKey) localStorage.setItem(KEY_STORE, state.apiKey);
      else localStorage.removeItem(KEY_STORE);
      updateKeyHint();
      $("settingsDlg").close();
      toast(state.apiKey ? "API key saved" : "API key cleared");
    });

    // Keyboard: space = play/pause (when not typing)
    document.addEventListener("keydown", (e) => {
      if (e.code === "Space" && document.activeElement.tagName !== "INPUT") {
        e.preventDefault();
        togglePlay();
      }
    });
  }

  // =========================================================================
  // Init
  // =========================================================================
  function init() {
    bind();
    renderPlaylists();
    updateKeyHint();
    setPlayingUI(false);
    loadYouTubeApi();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
