const API_BASE = '';  // 같은 서버에서 서빙하므로 상대 경로 사용
const main = document.getElementById('main-content');

// ── 아카이브 뷰 ──────────────────────────────────────────
async function fetchArchive() {
  main.innerHTML = '<div class="loading">불러오는 중...</div>';
  try {
    const res = await fetch(`${API_BASE}/posts/archive`);
    if (!res.ok) throw new Error('서버 오류');
    const data = await res.json();
    renderArchive(data);
    setAuthorName(data);
  } catch (e) {
    main.innerHTML = '<div class="loading">불러오기 실패: ' + e.message + '</div>';
  }
}

function renderArchive(yearGroups) {
  if (!yearGroups || yearGroups.length === 0) {
    main.innerHTML = '<div class="loading">등록된 일기가 없습니다.</div>';
    return;
  }

  const html = yearGroups.map(yearGroup => `
    <div class="archive-year">${yearGroup.year}년</div>
    ${yearGroup.months.map(monthGroup => `
      <div class="archive-month">${monthGroup.month}월</div>
      ${monthGroup.days.map(dayGroup => {
        const dateStr = dayGroup.date;
        const day = parseInt(dateStr.split('-')[2]);
        return dayGroup.posts.map(post => `
          <div class="archive-day">
            <span class="archive-day-title" onclick="fetchPost(${post.postId})">${day}일</span>
            <div class="archive-day-preview" onclick="fetchPost(${post.postId})">${post.title}</div>
          </div>
        `).join('');
      }).join('')}
    `).join('')}
  `).join('');

  main.innerHTML = html;
}

function setAuthorName(yearGroups) {
  // 첫 번째 게시글의 작성자 이름을 사이드바에 표시
  // 상세 조회 시 author 정보가 있으므로 여기선 생략하고 상세 조회 시 업데이트
}

// ── 상세 뷰 ──────────────────────────────────────────────
async function fetchPost(id) {
  main.innerHTML = '<div class="loading">불러오는 중...</div>';
  try {
    const res = await fetch(`${API_BASE}/posts/${id}`);
    if (!res.ok) throw new Error('서버 오류');
    const data = await res.json();
    renderPost(data);
  } catch (e) {
    main.innerHTML = '<div class="loading">불러오기 실패: ' + e.message + '</div>';
  }
}

function renderPost(post) {
  const dateObj = new Date(post.createdAt);
  const dateLabel = `${dateObj.getFullYear()}년 ${dateObj.getMonth() + 1}월 ${dateObj.getDate()}일`;

  // 작성자 이름 사이드바 업데이트
  if (post.author && post.author.nickname) {
    document.getElementById('author-name').textContent = post.author.nickname;
  }

  const contentHtml = marked.parse(post.content || '');

  main.innerHTML = `
    <button class="post-back" onclick="fetchArchive()">← 목록으로</button>
    <div class="post-date">${dateLabel}</div>
    <div class="post-title">${escapeHtml(post.title)}</div>
    <div class="post-content">${contentHtml}</div>
  `;
}

// ── 유틸 ─────────────────────────────────────────────────
function escapeHtml(text) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// ── 네비게이션 ────────────────────────────────────────────
document.getElementById('nav-diary').addEventListener('click', e => {
  e.preventDefault();
  fetchArchive();
});

document.getElementById('nav-posts').addEventListener('click', e => {
  e.preventDefault();
  fetchArchive(); // 추후 글 목록 뷰로 교체
});

// ── 초기 로드 ─────────────────────────────────────────────
fetchArchive();
