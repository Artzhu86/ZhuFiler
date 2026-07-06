import { initLayout } from '../layout'
import { fetchRepoStats, fetchContributors, animateNumber, setStatsFallback } from '../lib/github'

initLayout('about')

/* ---------- 数据 ---------- */
const STAT_CARDS = [
  { id: 'about-stars', icon: 'star', label: 'GitHub Stars', tip: '被这么多人点了星，压力有点大呢' },
  { id: 'about-forks', icon: 'fork_right', label: 'Forks', tip: '有人想要改造它，这是好事' },
  { id: 'about-watchers', icon: 'visibility', label: 'Watchers', tip: '有人在默默关注着，真好' },
]

const HISTORY = [
  {
    date: '2026年6月27日 · v1.5',
    title: '第一次构想',
    desc: '上传了 ZhuFiler_1.5.zip,这是项目的最初形态——一个简单的 Android 文件管理器。那是 2026 年的夏天,梦想的种子第一次种下。',
  },
  {
    date: '2026年6月28日 · v1.6',
    title: '一切的开始',
    desc: '第一次提交代码到 Git 仓库。"ZhuFiler v1.6" 这行 commit 消息,是 ZhuFiler 真正意义上的开始。',
  },
  {
    date: '2026年6月29日 · v1.7',
    title: '仓库上线',
    desc: '创建 README.md,项目正式在 GitHub 上有了门面。虽然内容还很简陋,但它告诉世界:ZhuFiler 来了。',
  },
  {
    date: '2026年7月2日 · v1.8',
    title: '功能逐渐完善',
    desc: '书签系统、多选操作、文件列表重构,还有 `MultiSelectController`、`BookmarkManager`、`FabManager` 等核心模块上线。文件管理器该有的功能基本都有了。',
  },
  {
    date: '2026年7月4日 · v1.9',
    title: '代码高亮 + 图片缩放',
    desc: '集成 TextMate 语法资源,支持 C/C++/CSS/HTML/Java/JavaScript/JSON/Kotlin/Lua/Markdown/Python/XML 等 13 种语言代码高亮;引入 PhotoView 库,图片查看支持双指缩放。',
  },
  {
    date: '2026年7月5日 · MIT 协议',
    title: '正式开源',
    desc: '添加 MIT License 文件。ZhuFiler 正式成为开源项目,任何人都可以自由使用、修改和分享。',
  },
]

const STORIES = [
  { icon: 'auto_stories', title: '为什么要做 ZhuFiler？', body: '<p>市面上的文件管理器要么太丑,要么太复杂,要么就是广告满天飞。</p><p>我只是想做一个<strong>干净、好看、好用</strong>的文件管理器。就这么简单。</p>' },
  { icon: 'code', title: '技术选型', body: '<p>为什么用 Kotlin？因为 Kotlin 写起来舒服啊！</p><p>为什么用 Material 3？因为 Material You 的动态颜色太好看了！</p><p>为什么不用 XML 布局？因为代码写 UI 更灵活,也更符合 Kotlin 的风格。</p>' },
  { icon: 'favorite', title: '未来展望', body: '<p>ZhuFiler 还在成长中。未来的计划：</p>', chips: ['更多文件格式支持', '云端同步', '插件系统', '更多主题', '性能优化'] },
]

const LINKS = [
  { icon: 'code', title: 'GitHub 仓库', desc: '源代码、Issue、Releases', href: 'https://github.com/Artzhu86/ZhuFiler', external: true },
  { icon: 'download', title: '下载', desc: 'APK 直接下载', href: 'download.html' },
  { icon: 'menu_book', title: '文档', desc: '使用指南', href: 'guide.html' },
  { icon: 'developer_mode', title: '开发', desc: '参与贡献', href: 'development.html' },
]

/* ---------- 渲染 ---------- */
const main = document.getElementById('main-content')!
main.innerHTML = `
  <div class="page-header">
    <div class="container">
      <mdui-icon name="info" class="page-header-icon animate-in"></mdui-icon>
      <h1 class="page-title animate-in delay-1">关于 ZhuFiler</h1>
      <p class="page-desc animate-in delay-2">你正在使用的是什么？</p>
    </div>
  </div>

  <div class="section">
    <div class="container-wide">

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="text-align: center; margin-bottom: 16px;">
        <div class="mdui-prose zf-prose-center">
          <p>ZhuFiler（烛文件）是一个由 <a href="https://github.com/Artzhu86" target="_blank" rel="noopener">Artzhu</a> 用爱（和 Kotlin）打造的 Android 文件管理器。</p>
          <p>不是那种让人头疼的老古董，而是一个真正想让你用得开心的工具。</p>
        </div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="bar_chart"></mdui-icon>
        项目数据
      </h2>

      <div class="grid grid-auto-sm" style="margin-bottom: 16px;">
        ${STAT_CARDS.map(
          (s, i) => `
          <mdui-card class="stat-card animate-scale delay-${i + 1}" variant="elevated">
            <mdui-icon name="${s.icon}"></mdui-icon>
            <div class="stat-value" id="${s.id}">--</div>
            <div class="stat-label">${s.label}</div>
            <div class="stat-label zf-stat-tip">${s.tip}</div>
          </mdui-card>
        `
        ).join('')}
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="timeline"></mdui-icon>
        发展历程
      </h2>

      <div class="zf-timeline">
        ${HISTORY.map(
          (h, i) => `
          <div class="zf-timeline-item animate-in delay-${i + 1}">
            <div class="zf-timeline-date">${h.date}</div>
            <h3>${h.title}</h3>
            <p>${h.desc}</p>
          </div>
        `
        ).join('')}
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="group"></mdui-icon>
        贡献者
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="text-align: center; margin-bottom: 16px;">
        <div class="zf-contributor-main">
          <mdui-avatar src="https://github.com/Artzhu86.png?size=128" label="Artzhu"></mdui-avatar>
          <div class="zf-contributor-name">Artzhu</div>
          <div class="zf-contributor-role">项目创始人</div>
          <mdui-button href="https://github.com/Artzhu86" target="_blank" rel="noopener" icon="code" variant="tonal">GitHub</mdui-button>
        </div>
        <div id="contributors-list" class="zf-contributors zf-loading-text">加载中...</div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="lightbulb"></mdui-icon>
        开发故事
      </h2>

      ${STORIES.map(
        (s, i) => `
        <mdui-card class="zf-card animate-in delay-${i + 1}" variant="filled" style="margin-bottom: 16px;">
          <h3 class="zf-subtitle-with-icon">
            <mdui-icon name="${s.icon}" class="zf-tone-primary"></mdui-icon>
            ${s.title}
          </h3>
          <div class="mdui-prose">${s.body}</div>
          ${s.chips ? `<div class="zf-chip-row">${s.chips.map((c) => `<mdui-chip variant="tonal">${c}</mdui-chip>`).join('')}</div>` : ''}
          ${i === STORIES.length - 1 ? '<div class="mdui-prose zf-prose-cta"><p>如果你有什么建议，欢迎提交 Issue！</p></div>' : ''}
        </mdui-card>
      `
      ).join('')}

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="link"></mdui-icon>
        相关链接
      </h2>

      <div class="grid grid-4 zf-link-grid">
        ${LINKS.map(
          (l, i) => `
          <mdui-card class="mini-card animate-in delay-${i + 1}" variant="filled" clickable href="${l.href}" ${l.external ? 'target="_blank" rel="noopener"' : ''}>
            <mdui-icon name="${l.icon}"></mdui-icon>
            <div class="mini-card-title">${l.title}</div>
            <div class="mini-card-desc">${l.desc}</div>
          </mdui-card>
        `
        ).join('')}
      </div>

      <mdui-card class="zf-card animate-in" variant="filled" style="text-align: center; margin-bottom: 16px;">
        <h3 style="font-size: 20px; margin-bottom: 8px;">联系方式</h3>
        <p style="color: var(--mdui-color-on-surface-variant); margin-bottom: 16px;">如果你有任何问题、建议或只是想打个招呼：</p>
        <div class="cta-actions">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler/issues" target="_blank" rel="noopener" icon="code" variant="filled">GitHub Issues</mdui-button>
        </div>
      </mdui-card>

      <div class="cta-section animate-scale">
        <h2>用心打造</h2>
        <p>ZhuFiler 是一个个人项目，用心打造。如果它对你有帮助，那我就很开心了。</p>
        <div class="cta-actions">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener" icon="star" variant="filled">给个 Star 吧</mdui-button>
        </div>
      </div>
    </div>
  </div>
`

/* ---------- 数据加载(使用共享模块 + 完善错误处理) ---------- */
async function loadAboutData() {
  // 1. 仓库统计
  const stats = await fetchRepoStats()
  if (stats) {
    animateNumber('about-stars', stats.stars)
    animateNumber('about-forks', stats.forks)
    animateNumber('about-watchers', stats.watchers)
  } else {
    setStatsFallback(['about-stars', 'about-forks', 'about-watchers'])
  }

  // 2. 贡献者(带完整错误处理)
  const container = document.getElementById('contributors-list')
  if (!container) return
  const contribs = await fetchContributors()
  if (contribs.length === 0) {
    container.className = 'zf-contributors zf-error-text'
    container.textContent = '暂无其他贡献者'
    return
  }
  if (contribs.length <= 1) {
    container.remove()
    return
  }
  // 用 DOM API 替代字符串拼接
  container.className = 'zf-contributors'
  container.innerHTML = ''
  const HIDDEN = ['superman32432432']
  contribs.slice(1).filter((c) => !HIDDEN.includes(c.login)).forEach((c) => {
    const a = document.createElement('a')
    a.href = c.html_url
    a.target = '_blank'
    a.rel = 'noopener'
    a.className = 'zf-contributor'
    a.innerHTML = `
      <mdui-avatar src="${c.avatar_url}?size=48" label="${c.login}"></mdui-avatar>
      <span>${c.login}</span>
    `
    container.appendChild(a)
  })
}
loadAboutData()
