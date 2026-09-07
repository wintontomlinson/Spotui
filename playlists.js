/*
 * Curated featured playlists. These use real YouTube video IDs so the site
 * works fully even WITHOUT a YouTube Data API key. Add a key in Settings to
 * unlock live search on top of this.
 *
 * Each track: { title, artist, videoId, duration (seconds, optional) }
 * The cover art for a playlist falls back to the first track's YouTube thumbnail.
 */
window.FEATURED_PLAYLISTS = [
  {
    id: "global-hits",
    title: "Global Hits",
    description: "Chart-topping tracks from around the world.",
    tracks: [
      { title: "Blinding Lights", artist: "The Weeknd", videoId: "4NRXx6U8ABQ" },
      { title: "Shape of You", artist: "Ed Sheeran", videoId: "JGwWNGJdvx8" },
      { title: "Levitating", artist: "Dua Lipa", videoId: "TUVcZfQe-Kw" },
      { title: "Uptown Funk", artist: "Mark Ronson ft. Bruno Mars", videoId: "OPf0YbXqDm0" },
      { title: "Someone You Loved", artist: "Lewis Capaldi", videoId: "zABLecsR5UE" },
      { title: "Bad Guy", artist: "Billie Eilish", videoId: "DyDfgMOUjCI" },
    ],
  },
  {
    id: "bollywood",
    title: "Bollywood Beats",
    description: "Popular Hindi film & pop songs.",
    tracks: [
      { title: "Kesariya", artist: "Arijit Singh", videoId: "BddP6PYo2gs" },
      { title: "Kal Ho Naa Ho", artist: "Sonu Nigam", videoId: "H4Tmb-Qep0o" },
      { title: "Tum Hi Ho", artist: "Arijit Singh", videoId: "Umqb9KENgmk" },
      { title: "Gallan Goodiyaan", artist: "Various Artists", videoId: "jCEdTq3j-0U" },
      { title: "Channa Mereya", artist: "Arijit Singh", videoId: "284Ov7ysmfA" },
    ],
  },
  {
    id: "chill-lofi",
    title: "Chill & Lo-Fi",
    description: "Relaxed beats to study or unwind.",
    tracks: [
      { title: "lofi hip hop radio mix", artist: "Lofi Girl", videoId: "5qap5aO4i9A" },
      { title: "Snowman", artist: "WYS", videoId: "vh5dtQ7bfoA" },
      { title: "Sunset Lover", artist: "Petit Biscuit", videoId: "4Hi7t2Rc2Xo" },
      { title: "Weightless", artist: "Marconi Union", videoId: "UfcAVejslrU" },
    ],
  },
  {
    id: "rock-classics",
    title: "Rock Classics",
    description: "Timeless anthems that never get old.",
    tracks: [
      { title: "Bohemian Rhapsody", artist: "Queen", videoId: "fJ9rUzIMcZQ" },
      { title: "Sweet Child O' Mine", artist: "Guns N' Roses", videoId: "1w7OgIMMRc4" },
      { title: "Smells Like Teen Spirit", artist: "Nirvana", videoId: "hTWKbfoikeg" },
      { title: "Hotel California", artist: "Eagles", videoId: "EqPtz5qN7HM" },
      { title: "Back In Black", artist: "AC/DC", videoId: "pAgnJDJN4VA" },
    ],
  },
  {
    id: "workout",
    title: "Workout Energy",
    description: "High-tempo tracks to power your session.",
    tracks: [
      { title: "Stronger", artist: "Kanye West", videoId: "PsO6ZnUZI0g" },
      { title: "Till I Collapse", artist: "Eminem", videoId: "ytQ5CYE1VZw" },
      { title: "Believer", artist: "Imagine Dragons", videoId: "7wtfhZwyrcc" },
      { title: "Can't Hold Us", artist: "Macklemore & Ryan Lewis", videoId: "2zNSgSzhBfM" },
    ],
  },
  {
    id: "throwback",
    title: "Throwback Pop",
    description: "The songs that defined the 2000s–2010s.",
    tracks: [
      { title: "Rolling in the Deep", artist: "Adele", videoId: "rYEDA3JcQqw" },
      { title: "Poker Face", artist: "Lady Gaga", videoId: "bESGLojNYSo" },
      { title: "Just Dance", artist: "Lady Gaga", videoId: "2Abk1jAONjw" },
      { title: "Umbrella", artist: "Rihanna", videoId: "CvBfHwUxHIk" },
      { title: "Viva La Vida", artist: "Coldplay", videoId: "dvgZkm1xWPE" },
    ],
  },
];
