const API = '';

const state = {
  view: 'home',
  year: null,
  categoryId: null,
  backView: null,
  categories: [],
  diaryCategory: null,
  archiveData: [],
  years: [],
  user: null,
  token: null
};

const main       = document.getElementById('main-content');
const authorEl   = document.getElementById('author-name');
const yearListEl = document.getElementById('year-list');
const catListEl  = document.getElementById('category-list');
const btnWrite   = document.getElementById('btn-write');

// ── 인증 ────────────────────────────────────────────────────
function loadAuth() {
  const token = localStorage.getItem('token');
  const user  = JSON.parse(localStorage.getItem('user') || 'null');
  if (token && user) {
    state.token = token;
    state.user  = user;
  }
  updateAuthUI();
}

function updateAuthUI() {
  if (state.user) {
    authorEl.textContent = state.user.nickname || state.user.username;
    authorEl.classList.add('logged-in');
    btnWrite.style.display = 'flex';
  } else {
    authorEl.textContent = 'Guest';
    authorEl.classList.remove('logged-in');
    btnWrite.style.display = 'none';
  }
}

function onAuthorClick() {
  if (state.user) {
    if (confirm(`${state.user.nickname || state.user.username} 으로 로그인 중입니다.\n로그아웃 하시겠습니까?`)) {
      state.token = null;
      state.user  = null;
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      updateAuthUI();
    }
  } else {
    openModal('login-modal');
    document.getElementById('login-id').focus();
  }
}

async function handleLogin() {
  const username = document.getElementById('login-id').value.trim();
  const password = document.getElementById('login-pw').value;
  const errEl    = document.getElementById('login-error');
  errEl.textContent = '';

  if (!username || !password) {
    errEl.textContent = '아이디와 비밀번호를 입력하세요.';
    return;
  }

  try {
    const res = await fetch(`${API}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) {
      errEl.textContent = '아이디 또는 비밀번호가 올바르지 않습니다.';
      return;
    }
    const data = await res.json();
    state.token = data.token;
    state.user  = data.user;
    localStorage.setItem('token', data.token);
    localStorage.setItem('user', JSON.stringify(data.user));
    closeModal('login-modal');
    document.getElementById('login-id').value = '';
    document.getElementById('login-pw').value = '';
    updateAuthUI();
    await refreshArchive();
  } catch {
    errEl.textContent = '로그인 중 오류가 발생했습니다.';
  }
}

// ── 일기 쓰기 모달 ──────────────────────────────────────────
function openWriteModal() {
  const today = new Date().toISOString().split('T')[0];
  document.getElementById('write-date').value = today;
  document.getElementById('write-title').value = '';
  document.getElementById('write-content').value = '';
  document.getElementById('write-public').checked = true;
  document.getElementById('write-error').textContent = '';
  openModal('write-modal');
  document.getElementById('write-title').focus();
}

async function handleWrite() {
  const title    = document.getElementById('write-title').value.trim();
  const postDate = document.getElementById('write-date').value;
  const content  = document.getElementById('write-content').value.trim();
  const isPublic = document.getElementById('write-public').checked;
  const errEl    = document.getElementById('write-error');
  errEl.textContent = '';

  if (!postDate) { errEl.textContent = '날짜를 선택하세요.'; return; }
  if (!content)  { errEl.textContent = '내용을 입력하세요.'; return; }

  const [y, m, d] = postDate.split('-');
  const finalTitle = title || `${parseInt(y)}년 ${parseInt(m)}월 ${parseInt(d)}일`;

  try {
    const res = await fetch(`${API}/posts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${state.token}`
      },
      body: JSON.stringify({
        title: finalTitle,
        content,
        isPublic,
        categoryId: state.diaryCategory?.id || null,
        tagIds: [],
        postDate
      })
    });
    if (!res.ok) {
      errEl.textContent = '저장 중 오류가 발생했습니다.';
      return;
    }
    const post = await res.json();
    closeModal('write-modal');
    await refreshArchive();
    showPost(post.id, 'diary');
  } catch {
    errEl.textContent = '저장 중 오류가 발생했습니다.';
  }
}

// ── 모달 공통 ────────────────────────────────────────────────
function openModal(id) {
  document.getElementById(id).classList.add('open');
}

function closeModal(id) {
  document.getElementById(id).classList.remove('open');
}

function authHeaders() {
  return state.token ? { 'Authorization': `Bearer ${state.token}` } : {};
}

function onOverlayClick(e, id) {
  if (e.target === e.currentTarget) closeModal(id);
}

// ── 초기화 ─────────────────────────────────────────────────
async function init() {
  loadAuth();
  try {
    const [cats, allArchive] = await Promise.all([
      fetch(`${API}/categories`).then(r => r.json()),
      fetch(`${API}/posts/archive`, { headers: authHeaders() }).then(r => r.json())
    ]);

    state.categories    = cats;
    state.diaryCategory = cats.find(c => c.name === '일기');

    if (state.diaryCategory) {
      state.archiveData = await fetch(`${API}/posts/archive?categoryId=${state.diaryCategory.id}`, { headers: authHeaders() })
        .then(r => r.json());
    } else {
      state.archiveData = allArchive;
    }

    state.years = state.archiveData.map(yg => yg.year);
    populateSidebar();
    await showHome();
  } catch (e) {
    main.innerHTML = `<div class="loading">불러오기 실패: ${e.message}</div>`;
  }
}

async function refreshArchive() {
  try {
    if (state.diaryCategory) {
      state.archiveData = await fetch(`${API}/posts/archive?categoryId=${state.diaryCategory.id}`, { headers: authHeaders() })
        .then(r => r.json());
    } else {
      state.archiveData = await fetch(`${API}/posts/archive`, { headers: authHeaders() }).then(r => r.json());
    }
    state.years = state.archiveData.map(yg => yg.year);
    populateSidebar();
  } catch {}
}

// ── 사이드바 구성 ───────────────────────────────────────────
function populateSidebar() {
  yearListEl.innerHTML = state.years.map(y =>
    `<a href="#" class="year-item" data-year="${y}">${y}년</a>`
  ).join('');

  yearListEl.querySelectorAll('.year-item').forEach(el => {
    el.addEventListener('click', e => {
      e.preventDefault();
      showDiaryArchive(parseInt(el.dataset.year));
    });
  });

  const others = state.categories.filter(c => c.name !== '일기');
  catListEl.innerHTML = others.map(c =>
    `<a href="#" class="nav-item nav-category" data-id="${c.id}">${c.name}</a>`
  ).join('');

  catListEl.querySelectorAll('.nav-category').forEach(el => {
    el.addEventListener('click', e => {
      e.preventDefault();
      showCategoryPosts(parseInt(el.dataset.id), el.textContent.trim());
    });
  });
}

// ── 홈 뷰 ──────────────────────────────────────────────────
async function showHome() {
  setActiveNav('home');
  state.view = 'home';

  if (!state.archiveData.length) {
    main.innerHTML = '<div class="loading">등록된 일기가 없습니다.</div>';
    return;
  }

  const firstEntry = state.archiveData[0].months[0].days[0].posts[0];
  main.innerHTML = '<div class="loading">불러오는 중...</div>';

  try {
    const post = await fetch(`${API}/posts/${firstEntry.postId}`, { headers: authHeaders() }).then(r => r.json());
    renderHomePost(post);
  } catch {
    main.innerHTML = '<div class="loading">불러오기 실패</div>';
  }
}

function renderHomePost(post) {
  main.innerHTML = `
    <div class="home-date">${formatDate(post.postDate)}</div>
    <div class="home-content">${marked.parse(post.content || '')}</div>
  `;
}

// ── 일기 아카이브 뷰 ────────────────────────────────────────
function showDiaryArchive(year) {
  setActiveNav('diary', null, year);
  state.view = 'diary';
  state.year = year;

  const yearGroup = state.archiveData.find(yg => yg.year === year);
  if (!yearGroup) {
    main.innerHTML = `<div class="loading">${year}년 일기가 없습니다.</div>`;
    return;
  }

  const html = `
    <div class="archive-header">${year}년 일기</div>
    ${yearGroup.months.map(mg => `
      <div class="archive-month">${mg.month}월</div>
      ${mg.days.map(dg => {
        const day = parseInt(dg.date.split('-')[2]);
        return dg.posts.map(p => `
          <div class="archive-entry" onclick="showPost(${p.postId}, 'diary')">
            <span class="archive-entry-day">${day}일</span>
            <span class="archive-entry-title">${escHtml(p.title)}</span>
          </div>
        `).join('');
      }).join('')}
    `).join('')}
  `;

  main.innerHTML = html;
}

// ── 카테고리 목록 뷰 ────────────────────────────────────────
async function showCategoryPosts(categoryId, categoryName) {
  setActiveNav('category', categoryId);
  state.view      = 'category';
  state.categoryId = categoryId;
  main.innerHTML  = '<div class="loading">불러오는 중...</div>';

  try {
    const data = await fetch(`${API}/posts?categoryId=${categoryId}&size=50&sort=postDate,desc`, { headers: authHeaders() })
      .then(r => r.json());
    renderCategoryList(data.content || [], categoryName);
  } catch {
    main.innerHTML = '<div class="loading">불러오기 실패</div>';
  }
}

function renderCategoryList(posts, categoryName) {
  if (!posts.length) {
    main.innerHTML = `<div class="loading">${categoryName}에 등록된 글이 없습니다.</div>`;
    return;
  }

  const html = `
    <div class="archive-header">${categoryName}</div>
    ${posts.map(p => `
      <div class="category-entry" onclick="showPost(${p.id}, 'category')">
        <div class="category-entry-date">${formatDate(p.postDate)}</div>
        <div class="category-entry-title">${escHtml(p.title)}</div>
      </div>
    `).join('')}
  `;

  main.innerHTML = html;
}

// ── 게시글 상세 뷰 ──────────────────────────────────────────
async function showPost(id, backView) {
  state.backView = backView;
  main.innerHTML = '<div class="loading">불러오는 중...</div>';

  try {
    const post = await fetch(`${API}/posts/${id}`, { headers: authHeaders() }).then(r => r.json());
    renderPostDetail(post);
    state.view = 'post';
  } catch {
    main.innerHTML = '<div class="loading">불러오기 실패</div>';
  }
}

function renderPostDetail(post) {
  main.innerHTML = `
    <button class="post-back" onclick="goBack()">← 목록으로</button>
    <div class="post-date">${formatDate(post.postDate)}</div>
    <div class="post-title">${escHtml(post.title)}</div>
    <div class="post-content">${marked.parse(post.content || '')}</div>
  `;
}

// ── 뒤로가기 ────────────────────────────────────────────────
function goBack() {
  if (state.backView === 'diary') {
    showDiaryArchive(state.year || state.years[0]);
  } else if (state.backView === 'category') {
    const cat = state.categories.find(c => c.id === state.categoryId);
    showCategoryPosts(state.categoryId, cat ? cat.name : '목록');
  } else {
    showHome();
  }
}

// ── 사이드바 활성 상태 ─────────────────────────────────────
function setActiveNav(type, categoryId, year) {
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.year-item').forEach(el => el.classList.remove('active'));

  if (type === 'home') {
    document.getElementById('nav-home').classList.add('active');
  } else if (type === 'diary') {
    document.getElementById('nav-diary').classList.add('active');
    if (year) {
      const el = document.querySelector(`.year-item[data-year="${year}"]`);
      if (el) el.classList.add('active');
    }
  } else if (type === 'category') {
    const el = document.querySelector(`.nav-category[data-id="${categoryId}"]`);
    if (el) el.classList.add('active');
  }
}

// ── 유틸 ────────────────────────────────────────────────────
function formatDate(postDate) {
  if (!postDate) return '';
  const [y, m, d] = postDate.split('-');
  return `${parseInt(y)}년 ${parseInt(m)}월 ${parseInt(d)}일`;
}

function escHtml(text) {
  return (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ── 네비게이션 이벤트 ─────────────────────────────────────
document.getElementById('nav-home').addEventListener('click', e => {
  e.preventDefault();
  showHome();
});

document.getElementById('nav-diary').addEventListener('click', e => {
  e.preventDefault();
  if (state.years.length) showDiaryArchive(state.years[0]);
});

// ── 시작 ────────────────────────────────────────────────────
init();
