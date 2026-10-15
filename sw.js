// Офлайн-кэш: приложение работает без интернета (кроме отправки в Claude).
const CACHE = 'wd-v7';
const ASSETS = ['./', 'index.html', 'style.css', 'app.js', 'manifest.json', 'icons/icon.svg', 'icons/icon-192.png', 'icons/icon-512.png'];

self.addEventListener('install', (e) => {
  // cache: 'reload' — при установке берём свежие файлы с сервера, а не из HTTP-кэша
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(ASSETS.map((u) => new Request(u, { cache: 'reload' })))));
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))));
  self.clients.claim();
});

// Сначала сеть (чтобы обновления подхватывались), при отсутствии сети — кэш.
// cache: 'no-cache' — не брать устаревшие файлы из HTTP-кэша браузера
// (GitHub Pages разрешает кэшировать их до 10 минут), а сверяться с сервером.
self.addEventListener('fetch', (e) => {
  if (e.request.method !== 'GET' || new URL(e.request.url).origin !== location.origin) return;
  e.respondWith(
    fetch(e.request.url, { cache: 'no-cache' }) // по URL: навигационный Request нельзя переопределять
      .then((res) => {
        const copy = res.clone();
        caches.open(CACHE).then((c) => c.put(e.request, copy));
        return res;
      })
      .catch(() => caches.match(e.request, { ignoreSearch: true }))
  );
});
