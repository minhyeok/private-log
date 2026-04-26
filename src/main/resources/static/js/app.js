const API = '';

const state = {
  view: 'home',
  year: null,
  categoryId: null,
  backView: null,
  categories: [],
  diaryCategory: null,
  archiveData: [],
  years: []
};

const main      = document.getElementById('main-content');
const authorEl  = document.getElementById('author-name');
const yearListEl = document.getElementById('year-list');
const catListEl  = document.getElementById('category-list');

// ── 초기화 ─────────────────────────────────────────────────
async function init() {
  try {
    const [cats, allArchive] = await Promise.all([
      fetch(`${API}/categories`).then(r => r.json()),
      fetch(`${API}/posts/archive`).then(r => r.json())
    ]);

    state.categories = cats;
    state.diaryCategory = cats.find(c => c.name === '일기');

    // 일기 카테고리가 있으면 해당 아카이브만, 없으면 전체
    if (state.diaryCategory) {
      state.archiveData = await fetch(`${API}/posts/archive?categoryId=${state.diaryCategory.id}`)
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

// ── 사이드바 구성 ───────────────────────────────────────────
function populateSidebar() {
  // 연도 목록
  yearListEl.innerHTML = state.years.map(y =>
    `<a href="#" class="year-item" data-year="${y}">${y}년</a>`
  ).join('');

  yearListEl.querySelectorAll('.year-item').forEach(el => {
    el.addEventListener('click', e => {
      e.preventDefault();
      showDiaryArchive(parseInt(el.dataset.year));
    });
  });

  // 일기 외 카테고리
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

// ── 홈 뷰: 가장 최근 일기 ──────────────────────────────────
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
    const post = await fetch(`${API}/posts/${firstEntry.postId}`).then(r => r.json());
    if (post.author?.nickname) authorEl.textContent = post.author.nickname;
    renderHomePost(post);
  } catch (e) {
    main.innerHTML = '<div class="loading">불러오기 실패</div>';
  }
}

function renderHomePost(post) {
  const d = new Date(post.createdAt);
  const dateLabel = `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;

  main.innerHTML = `
    <div class="home-date">${dateLabel}</div>
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
  state.view = 'category';
  state.categoryId = categoryId;
  main.innerHTML = '<div class="loading">불러오는 중...</div>';

  try {
    const data = await fetch(`${API}/posts?categoryId=${categoryId}&size=50&sort=createdAt,desc`)
      .then(r => r.json());
    renderCategoryList(data.content || [], categoryName);
  } catch (e) {
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
    ${posts.map(p => {
      const d = new Date(p.createdAt);
      const dateLabel = `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;
      return `
        <div class="category-entry" onclick="showPost(${p.id}, 'category')">
          <div class="category-entry-date">${dateLabel}</div>
          <div class="category-entry-title">${escHtml(p.title)}</div>
        </div>
      `;
    }).join('')}
  `;

  main.innerHTML = html;
}

// ── 게시글 상세 뷰 ──────────────────────────────────────────
async function showPost(id, backView) {
  state.backView = backView;
  main.innerHTML = '<div class="loading">불러오는 중...</div>';

  try {
    const post = await fetch(`${API}/posts/${id}`).then(r => r.json());
    if (post.author?.nickname) authorEl.textContent = post.author.nickname;
    renderPostDetail(post);
    state.view = 'post';
  } catch (e) {
    main.innerHTML = '<div class="loading">불러오기 실패</div>';
  }
}

function renderPostDetail(post) {
  const d = new Date(post.createdAt);
  const dateLabel = `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`;

  main.innerHTML = `
    <button class="post-back" onclick="goBack()">← 목록으로</button>
    <div class="post-date">${dateLabel}</div>
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
