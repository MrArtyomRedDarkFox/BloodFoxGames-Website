(() => {
  "use strict";
  const API = ""; // Nginx проксирует /api/* в game-store-service.
  const app = document.getElementById("app-root");
  const toast = document.getElementById("toast");
  const state = {
    page: (location.hash.replace(/^#\/?/, "") || "home"),
    user: readSession(),
    games: [],
    library: [],
    search: "",
    loadingGames: false,
    loadingLibrary: false,
    gamesError: "",
    libraryError: "",
    nextPage: "home"
  };
  let toastTimer;

  function readSession() {
    try {
      const value = JSON.parse(localStorage.getItem("bloodfox.session") || "null");
      if (value && Number.isFinite(Number(value.id)) && value.username) return value;
    } catch (_) { /* повреждённую сессию просто сбросим */ }
    return null;
  }
  function saveSession(user) {
    // Пароль намеренно не сохраняем. Для production-системы нужна серверная сессия/JWT.
    state.user = { id: Number(user.id), username: user.username, email: user.email || "" };
    localStorage.setItem("bloodfox.session", JSON.stringify(state.user));
  }
  function clearSession() {
    state.user = null;
    state.library = [];
    localStorage.removeItem("bloodfox.session");
  }
  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;", "'":"&#39;"}[char]));
  }
  function price(value) {
    const amount = Number(value);
    return Number.isFinite(amount) ? `$${amount.toFixed(2)}` : "Цена уточняется";
  }
  async function api(path, options = {}) {
    const response = await fetch(`${API}${path}`, {
      ...options,
      headers: { "Content-Type": "application/json", ...(options.headers || {}) }
    });
    const text = await response.text();
    let payload = null;
    if (text) {
      try { payload = JSON.parse(text); } catch (_) { payload = text; }
    }
    if (!response.ok) {
      const detail = payload && typeof payload === "object"
        ? (payload.detail || payload.message || payload.error || payload.title)
        : payload;
      throw new Error(detail || `Ошибка HTTP ${response.status}`);
    }
    return payload;
  }
  function notify(message, isError = false) {
    toast.textContent = message;
    toast.classList.toggle("error", isError);
    toast.classList.add("show");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.remove("show"), 3600);
  }
  function routeTo(page) {
    const valid = ["home", "catalog", "library", "login", "register"];
    const target = valid.includes(page) ? page : "home";
    if (location.hash === `#${target}`) route();
    else location.hash = target;
  }
  async function loadGames() {
    state.loadingGames = true;
    state.gamesError = "";
    try {
      const response = await api("/api/v1/games");
      state.games = Array.isArray(response) ? response : [];
    } catch (error) {
      state.gamesError = `Не удалось получить каталог: ${error.message}. Проверь, что backend запущен.`;
    } finally { state.loadingGames = false; }
  }
  async function loadLibrary() {
    if (!state.user) { state.library = []; state.libraryError = ""; return; }
    state.loadingLibrary = true;
    state.libraryError = "";
    try {
      const response = await api(`/api/v1/users/${Number(state.user.id)}/library`);
      state.library = Array.isArray(response) ? response : [];
    } catch (error) {
      state.libraryError = `Не удалось загрузить библиотеку: ${error.message}`;
    } finally { state.loadingLibrary = false; }
  }
  function logo() {
    return `<a class="brand" href="#home" aria-label="BloodFoxGames — главная">
      <span class="brand-icon" aria-hidden="true"><img src="/assets/bloodfox-logo.png" alt=""></span>
      <span class="brand-word">BLOOD<span>FOX</span>GAMES<span class="brand-caption">PLAY BEYOND THE DARK</span></span>
    </a>`;
  }
  function header() {
    const nav = [{id:"home",label:"Главная"},{id:"catalog",label:"Магазин"},{id:"library",label:"Библиотека"}];
    return `<header class="topbar"><div class="topbar-inner">
      ${logo()}
      <nav class="nav" id="main-nav" aria-label="Основная навигация">${nav.map(item => `<a class="nav-link ${state.page === item.id ? "active" : ""}" href="#${item.id}">${item.label}</a>`).join("")}</nav>
      <div class="topbar-spacer"></div>
      ${state.page === "catalog" ? `<label class="header-search"><span class="search-glyph">⌕</span><input id="header-search" aria-label="Поиск игр" placeholder="Найти игру…" value="${esc(state.search)}"></label>` : ""}
      <div class="account-area">${state.user
        ? `<a class="user-chip" href="#library"><span class="avatar">${esc(state.user.username.slice(0,1).toUpperCase())}</span><span class="username">${esc(state.user.username)}</span></a><button class="btn btn-ghost btn-small" data-action="logout">Выйти</button>`
        : `<a class="btn btn-ghost btn-small" href="#login">Войти</a><a class="btn btn-primary btn-small" href="#register">Создать аккаунт</a>`}</div>
      <button class="mobile-menu" id="mobile-menu" aria-label="Открыть меню" aria-expanded="false">☰</button>
    </div></header>`;
  }
  function footer() {
    return `<footer class="footer"><div><div class="footer-brand">BLOOD<span>FOX</span>GAMES</div><div>Независимый учебный проект · магазин и игровая библиотека</div></div><div>Работает на Java · PostgreSQL · Docker Compose</div></footer>`;
  }
  function shell(content) { app.innerHTML = `${header()}<main>${content}${footer()}</main>`; }
  function gameArt(game) {
    const title = String(game.title || "GAME");
    const normalized = title.toLowerCase();
    const coverClass = normalized.includes("lost") || normalized.includes("night") ? "cover-night" : normalized.includes("fox") ? "cover-fox" : "cover-default";
    const code = normalized.includes("lost") ? "DARK / 02" : normalized.includes("fox") ? "ARCADE / 01" : "BFG / ORIGINAL";
    const coverTitle = normalized.includes("fox") ? "FOX<br>RACES" : normalized.includes("lost") ? "LOST<br>NIGHT" : esc(title).replace(/\s+/g,"<br>");
    return `<div class="game-cover ${coverClass}"><span class="cover-code">${code}</span><div class="cover-title">${coverTitle}<small>BLOODFOX ORIGINALS</small></div></div>`;
  }
  function gameCard(game, options = {}) {
    const owned = options.owned || false;
    return `<article class="game-card">${gameArt(game)}<div class="game-body"><div class="game-title-row"><h3 class="game-title">${esc(game.title)}</h3>${owned ? `<span class="badge-owned">В БИБЛИОТЕКЕ</span>` : ""}</div>
      <p class="game-desc">${esc(game.description || "Описание этой игры появится позже.")}</p>
      <div class="game-footer"><div><span class="price">${price(game.price)}</span><span class="price-caption">${owned ? "Ваша копия" : "Цифровая версия"}</span></div>
      ${owned ? `<button class="btn btn-secondary btn-small" data-action="owned" disabled>✓ В коллекции</button>` : `<button class="btn btn-primary btn-small" data-action="buy" data-game-id="${Number(game.id)}">${state.user ? "Купить игру" : "Войти и купить"} <span aria-hidden="true">→</span></button>`}</div>
    </div></article>`;
  }
  function warning() { return state.gamesError ? `<div class="api-warning">${esc(state.gamesError)}</div>` : ""; }
  function homePage() {
    const featured = state.games[0];
    return `<section class="hero"><div class="hero-content"><span class="eyebrow">ТВОЯ ИГРОВАЯ ВСЕЛЕННАЯ</span><h1>Игры, в которые<br>хочется <span>пропасть.</span></h1><p class="hero-copy">Открывай новые миры, собирай личную коллекцию и возвращайся к любимым приключениям. Всё, что ты покупаешь, остаётся в твоей библиотеке BloodFoxGames.</p>
      <div class="hero-actions"><a class="btn btn-primary" href="#catalog">Открыть магазин <span>↗</span></a><a class="btn btn-secondary" href="#library">Моя библиотека <span>→</span></a></div>
      <div class="hero-meta"><span><i class="pulse-dot"></i> Игровой сервис онлайн</span><span>✦ Каталог и коллекция</span></div></div>
      <div class="hero-art" aria-label="Арт BloodFoxGames"><div class="hero-ribbon"></div><div class="hero-game-card"><div class="hero-card-top"><span>BFG / 001</span><span>NEW WORLDS</span></div><div class="hero-card-bottom"><div><strong>${featured ? esc(featured.title) : "BLOODFOX"}</strong><small>FIND YOUR NEXT ADVENTURE</small></div><span>✦</span></div></div><div class="floating-tag"><strong>${state.games.length} ${state.games.length === 1 ? "игра" : "игр"} в каталоге</strong><span>И твоя история только начинается</span></div></div>
    </section>
    <section class="section"><div class="section-head"><div><h2 class="section-title">Всё для твоей игровой жизни</h2><p class="section-subtitle">Магазин, покупки и коллекция — в одном месте.</p></div><a href="#catalog" class="text-link">Посмотреть магазин <span>→</span></a></div>
      <div class="feature-row"><div class="feature-tile"><div class="feature-icon">⌕</div><div><strong>Находи новое</strong><p>Смотри каталог и выбирай приключение.</p></div></div><div class="feature-tile"><div class="feature-icon">↗</div><div><strong>Покупай в один клик</strong><p>Добавляй игры в свою коллекцию.</p></div></div><div class="feature-tile"><div class="feature-icon">▤</div><div><strong>Собирай библиотеку</strong><p>Все приобретённые игры всегда под рукой.</p></div></div></div>
    </section>
    <section class="section"><div class="section-head"><div><h2 class="section-title">Выбрано для тебя</h2><p class="section-subtitle">Первые игры из каталога BloodFoxGames.</p></div><a href="#catalog" class="text-link">Весь каталог <span>→</span></a></div>${warning()}${state.loadingGames ? `<div class="loading">Загружаем игры…</div>` : state.games.length ? `<div class="game-grid">${state.games.slice(0,3).map(game => gameCard(game,{owned: state.user && state.library.some(item => Number(item.id)===Number(game.id))})).join("")}</div>` : `<div class="empty-state"><div class="empty-symbol">✦</div><h2>Каталог загружается</h2><p>Когда API магазина будет доступен, здесь появятся игры из PostgreSQL.</p><button class="btn btn-secondary" data-action="refresh">Повторить загрузку</button></div>`}</section>`;
  }
  function catalogPage() {
    const search = state.search.trim().toLowerCase();
    const games = state.games.filter(game => `${game.title || ""} ${game.description || ""}`.toLowerCase().includes(search));
    return `<section class="page-banner"><span class="page-kicker">BLOODFOX STORE</span><h1>Магазин игр</h1><p>Выбери мир, который захочешь исследовать. Покупки сохраняются в твоей личной библиотеке.</p></section>
      ${warning()}<div class="catalog-toolbar"><label class="search-wide"><span>⌕</span><input id="catalog-search" placeholder="Поиск по названию или описанию…" value="${esc(state.search)}" aria-label="Поиск по каталогу"></label><span class="result-count" id="result-count">Найдено игр: ${games.length}</span></div>
      <div id="catalog-grid">${state.loadingGames ? `<div class="loading">Загружаем каталог…</div>` : games.length ? `<div class="game-grid">${games.map(game => gameCard(game,{owned: state.user && state.library.some(item=>Number(item.id)===Number(game.id))})).join("")}</div>` : `<div class="empty-state"><div class="empty-symbol">⌕</div><h2>${state.games.length ? "Ничего не найдено" : "Пока нет игр"}</h2><p>${state.games.length ? "Попробуй изменить поисковый запрос." : "Не удалось получить список игр. Убедись, что Docker Compose запущен."}</p>${state.games.length ? "" : `<button class="btn btn-secondary" data-action="refresh">Повторить загрузку</button>`}</div>`}</div>`;
  }
  function libraryPage() {
    if (!state.user) return `<section class="page-banner"><span class="page-kicker">YOUR COLLECTION</span><h1>Твоя библиотека</h1><p>Всё, что ты приобрёл, собрано в одном месте.</p></section><div class="empty-state"><div class="empty-symbol">▤</div><h2>Войди, чтобы открыть библиотеку</h2><p>Авторизуйся или создай аккаунт. После покупки игра появится в твоей коллекции.</p><div class="hero-actions" style="justify-content:center;margin-top:0"><a class="btn btn-primary" href="#login">Войти</a><a class="btn btn-secondary" href="#register">Создать аккаунт</a></div></div>`;
    return `<section class="page-banner"><span class="page-kicker">YOUR COLLECTION</span><h1>Твоя библиотека</h1><p>Твои игры, твоя история. Продолжай приключение с того места, где остановился.</p></section>
      <div class="library-top"><div class="library-user"><div class="library-avatar">${esc(state.user.username.slice(0,1).toUpperCase())}</div><div><strong>${esc(state.user.username)}</strong><small>${esc(state.user.email || "Аккаунт BloodFoxGames")}</small></div></div><div class="library-stat"><strong>${state.library.length}</strong><span>игр в коллекции</span></div></div>
      ${state.libraryError ? `<div class="api-warning">${esc(state.libraryError)}</div>` : ""}
      ${state.loadingLibrary ? `<div class="loading">Загружаем библиотеку…</div>` : state.library.length ? `<div class="game-grid">${state.library.map(game => gameCard(game,{owned:true})).join("")}</div>` : `<div class="empty-state"><div class="empty-symbol">▤</div><h2>Библиотека пока пуста</h2><p>Найди игру в магазине и нажми «Купить игру». Она появится здесь автоматически.</p><a class="btn btn-primary" href="#catalog">Перейти в магазин <span>→</span></a></div>`}`;
  }
  function authPage(mode) {
    const register = mode === "register";
    return `<section class="auth-layout"><div class="auth-art"><a class="brand" href="#home"><span class="brand-icon"><img src="/assets/bloodfox-logo.png" alt=""></span><span class="brand-word">BLOOD<span>FOX</span>GAMES<span class="brand-caption">PLAY BEYOND THE DARK</span></span></a><div><h2>${register ? "Начни свою игровую историю." : "С возвращением, игрок."}</h2><p>${register ? "Создай аккаунт, открывай новые миры и собирай свою коллекцию." : "Войди в аккаунт, чтобы вернуться к играм из своей библиотеки."}</p></div><span class="eyebrow">YOUR NEXT ADVENTURE STARTS HERE</span></div>
      <div class="auth-form-wrap"><h1>${register ? "Создать аккаунт" : "Войти в BloodFoxGames"}</h1><p class="form-lead">${register ? "Всего пара полей — и можно выбирать первую игру." : "Введи данные своего аккаунта, чтобы открыть библиотеку."}</p>
      <form id="auth-form" data-mode="${mode}" autocomplete="${register ? "on" : "on"}">
        <label class="form-field"><span>Имя пользователя</span><input name="username" required minlength="3" maxlength="40" autocomplete="username" placeholder="Например, DarkFox" /></label>
        ${register ? `<label class="form-field"><span>Электронная почта</span><input name="email" type="email" required maxlength="160" autocomplete="email" placeholder="you@example.com" /></label>` : ""}
        <label class="form-field"><span>Пароль</span><input name="password" type="password" required minlength="6" maxlength="100" autocomplete="${register ? "new-password" : "current-password"}" placeholder="Не менее 6 символов" /></label>
        <button class="btn btn-primary form-submit" type="submit">${register ? "Создать аккаунт" : "Войти в аккаунт"} <span>→</span></button>
      </form>
      <p class="form-bottom">${register ? `Уже есть аккаунт? <button type="button" data-route="login">Войти</button>` : `Впервые здесь? <button type="button" data-route="register">Создать аккаунт</button>`}</p>
      <p class="form-note">Учебный прототип. Используй только тестовые данные, не повторяй пароль от важных сервисов.</p></div></section>`;
  }
  function render() {
    let content;
    if (state.page === "catalog") content = catalogPage();
    else if (state.page === "library") content = libraryPage();
    else if (state.page === "login" || state.page === "register") content = authPage(state.page);
    else { state.page = "home"; content = homePage(); }
    shell(content);
  }
  async function route() {
    state.page = location.hash.replace(/^#\/?/, "") || "home";
    if (!["home", "catalog", "library", "login", "register"].includes(state.page)) state.page = "home";
    await loadGames();
    if (state.page === "library") await loadLibrary();
    else if (state.user) await loadLibrary();
    render();
  }
  async function handleBuy(button) {
    if (!state.user) {
      state.nextPage = "catalog";
      notify("Войди или создай аккаунт, чтобы купить игру.");
      routeTo("login");
      return;
    }
    const id = Number(button.dataset.gameId);
    if (!Number.isFinite(id) || id <= 0) return;
    button.disabled = true;
    const oldText = button.textContent;
    button.textContent = "Обрабатываем…";
    try {
      const result = await api(`/api/v1/users/${Number(state.user.id)}/buy/${id}`, { method: "POST", body: "{}" });
      notify(typeof result === "string" ? result : "Игра добавлена в библиотеку!");
      await loadLibrary();
      render();
    } catch (error) {
      notify(`Не удалось купить игру: ${error.message}`, true);
      button.disabled = false;
      button.textContent = oldText;
    }
  }
  async function handleAuth(form) {
    const mode = form.dataset.mode;
    const data = Object.fromEntries(new FormData(form).entries());
    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = mode === "register" ? "Создаём аккаунт…" : "Проверяем…";
    try {
      const path = mode === "register" ? "/api/v1/users/register" : "/api/v1/users/login";
      const user = await api(path, { method: "POST", body: JSON.stringify(data) });
      if (!user || !user.id || !user.username) throw new Error("Сервер вернул неполные данные аккаунта.");
      saveSession(user);
      await loadLibrary();
      notify(mode === "register" ? "Аккаунт создан! Добро пожаловать в BloodFoxGames." : `С возвращением, ${user.username}!`);
      const destination = state.nextPage || "home";
      state.nextPage = "home";
      routeTo(destination);
    } catch (error) {
      notify(error.message || "Не удалось выполнить запрос.", true);
      button.disabled = false;
      button.textContent = mode === "register" ? "Создать аккаунт →" : "Войти в аккаунт →";
    }
  }
  app.addEventListener("click", async event => {
    const routeButton = event.target.closest("[data-route]");
    if (routeButton) { routeTo(routeButton.dataset.route); return; }
    const actionButton = event.target.closest("[data-action]");
    if (!actionButton) return;
    const action = actionButton.dataset.action;
    if (action === "buy") await handleBuy(actionButton);
    if (action === "logout") {
      clearSession();
      notify("Ты вышел из аккаунта.");
      routeTo("home");
    }
    if (action === "refresh") { await loadGames(); render(); }
  });
  app.addEventListener("submit", async event => {
    if (event.target.id === "auth-form") {
      event.preventDefault();
      await handleAuth(event.target);
    }
  });
  app.addEventListener("input", event => {
    const input = event.target;
    if (input.id === "catalog-search" || input.id === "header-search") {
      state.search = input.value;
      const current = input;
      const start = current.selectionStart;
      const end = current.selectionEnd;
      if (state.page === "catalog") {
        const holder = document.getElementById("catalog-grid");
        const count = document.getElementById("result-count");
        const search = state.search.trim().toLowerCase();
        const games = state.games.filter(game => `${game.title || ""} ${game.description || ""}`.toLowerCase().includes(search));
        if (holder) holder.innerHTML = games.length ? `<div class="game-grid">${games.map(game => gameCard(game,{owned: state.user && state.library.some(item=>Number(item.id)===Number(game.id))})).join("")}</div>` : `<div class="empty-state"><div class="empty-symbol">⌕</div><h2>Ничего не найдено</h2><p>Попробуй изменить поисковый запрос.</p></div>`;
        if (count) count.textContent = `Найдено игр: ${games.length}`;
      } else {
        render();
        const newInput = document.getElementById("header-search");
        if (newInput) { newInput.focus(); newInput.setSelectionRange(start, end); }
      }
    }
  });
  document.addEventListener("click", event => {
    const menu = document.getElementById("mobile-menu");
    if (menu && event.target.closest("#mobile-menu")) {
      const nav = document.getElementById("main-nav");
      const open = nav.classList.toggle("open");
      menu.setAttribute("aria-expanded", String(open));
    } else if (!event.target.closest("#main-nav") && menu) {
      const nav = document.getElementById("main-nav");
      nav?.classList.remove("open");
      menu.setAttribute("aria-expanded", "false");
    }
  });
  window.addEventListener("hashchange", route);
  route();
})();
