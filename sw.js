// Офлайн-кэш: приложение работает без интернета (кроме отправки в Claude).
const CACHE = 'wd-v9';
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

// Сначала сеть (чтобы обновления подхватывались), но не дольше NET_TIMEOUT:
// при плохой связи сразу отдаём сохранённую копию, а свежая докачается в фоне
// и будет использована при следующем открытии. Без сети вовсе — сразу копия.
// cache: 'no-cache' — не брать устаревшие файлы из HTTP-кэша браузера
// (GitHub Pages разрешает кэшировать их до 10 минут), а сверяться с сервером.
const NET_TIMEOUT = 2500;

self.addEventListener('fetch', (e) => {
  if (e.request.method !== 'GET' || new URL(e.request.url).origin !== location.origin) return;
  const cached = () => caches.match(e.request, { ignoreSearch: true });

  // по URL: навигационный Request нельзя переопределять
  const network = fetch(e.request.url, { cache: 'no-cache' }).then(async (res) => {
    if (res.ok) {
      const c = await caches.open(CACHE);
      await c.put(e.request, res.clone());
    }
    return res;
  });
  e.waitUntil(network.catch(() => {})); // дать фоновой загрузке завершиться

  e.respondWith(new Promise((resolve, reject) => {
    let settled = false;
    const answer = (res) => { if (!settled) { settled = true; clearTimeout(timer); resolve(res); } };
    const timer = setTimeout(async () => {
      const res = await cached();
      if (res) answer(res); // иначе продолжаем ждать сеть
    }, NET_TIMEOUT);
    network.then(answer, async () => {
      const res = await cached();
      if (res) answer(res);
      else if (!settled) { settled = true; clearTimeout(timer); reject(new Error('offline')); }
    });
  }));
});
