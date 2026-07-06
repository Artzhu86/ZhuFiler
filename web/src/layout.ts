import 'mdui/mdui.css'
import 'mdui'
import './styles.css'

/**
 * 初始化站点布局
 * 使用 mdui-layout 正确组织 top-app-bar / navigation-drawer / main
 */
export function initLayout(activePage: string) {
  const app = document.getElementById('app')!
  app.innerHTML = `
    <mdui-layout full-height>
      <mdui-top-app-bar>
        <mdui-button-icon icon="menu" id="menu-toggle"></mdui-button-icon>
        <mdui-top-app-bar-title>ZhuFiler</mdui-top-app-bar-title>
        <div style="flex-grow: 1;"></div>
        <mdui-button-icon icon="code" href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener"></mdui-button-icon>
        <mdui-button-icon icon="dark_mode" id="theme-toggle"></mdui-button-icon>
      </mdui-top-app-bar>

      <mdui-navigation-drawer id="nav-drawer" close-on-overlay-click close-on-esc>
        <div class="zf-drawer-header">
          <mdui-icon name="folder" style="font-size: 32px; color: var(--mdui-color-primary);"></mdui-icon>
          <div>
            <div style="font-size: 20px; font-weight: 600;">ZhuFiler</div>
            <div style="font-size: 12px; color: var(--mdui-color-on-surface-variant);">烛文件 · Android 文件管理器</div>
          </div>
        </div>
        <mdui-divider></mdui-divider>
        <mdui-list>
          <mdui-list-subheader>导航</mdui-list-subheader>
          <mdui-list-item href="index.html" ${activePage === 'home' ? 'active' : ''} icon="home" rounded>首页</mdui-list-item>
          <mdui-list-item href="download.html" ${activePage === 'download' ? 'active' : ''} icon="download" rounded>下载</mdui-list-item>
          <mdui-list-item href="features.html" ${activePage === 'features' ? 'active' : ''} icon="star" rounded>功能特性</mdui-list-item>
          <mdui-list-item href="guide.html" ${activePage === 'guide' ? 'active' : ''} icon="menu_book" rounded>使用指南</mdui-list-item>
          <mdui-list-subheader>更多</mdui-list-subheader>
          <mdui-list-item href="changelog.html" ${activePage === 'changelog' ? 'active' : ''} icon="history" rounded>更新日志</mdui-list-item>
          <mdui-list-item href="about.html" ${activePage === 'about' ? 'active' : ''} icon="info" rounded>关于</mdui-list-item>
          <mdui-list-item href="development.html" ${activePage === 'development' ? 'active' : ''} icon="code" rounded>开发文档</mdui-list-item>
        </mdui-list>
        <mdui-divider></mdui-divider>
        <div class="zf-drawer-footer">
          <span>v1.9 · MIT</span>
          <mdui-chip icon="check_circle" variant="tonal">最新</mdui-chip>
        </div>
      </mdui-navigation-drawer>

      <mdui-layout-main class="zf-layout-main">
        <div id="main-content"></div>
        <footer class="zf-footer">
      <div class="zf-footer-inner">
        <div class="zf-footer-grid">
          <div>
            <div class="zf-footer-brand">
              <mdui-icon name="folder" style="font-size: 22px;"></mdui-icon>
              ZhuFiler
            </div>
            <div class="zf-footer-tagline">
              一个有趣且专业的<br>Android 文件管理器<br>
              <span style="color: var(--mdui-color-primary);">Material You · Kotlin · 开源</span>
            </div>
          </div>
          <div>
            <div class="zf-footer-col-title">导航</div>
            <div class="zf-footer-links">
              <a href="index.html">首页</a>
              <a href="download.html">下载</a>
              <a href="features.html">功能特性</a>
              <a href="guide.html">使用指南</a>
            </div>
          </div>
          <div>
            <div class="zf-footer-col-title">更多</div>
            <div class="zf-footer-links">
              <a href="changelog.html">更新日志</a>
              <a href="about.html">关于</a>
              <a href="development.html">开发文档</a>
            </div>
          </div>
          <div>
            <div class="zf-footer-col-title">链接</div>
            <div class="zf-footer-links">
              <a href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener">GitHub</a>
              <a href="https://github.com/Artzhu86/ZhuFiler/releases" target="_blank" rel="noopener">Releases</a>
              <a href="https://github.com/Artzhu86/ZhuFiler/issues" target="_blank" rel="noopener">Issues</a>
            </div>
          </div>
        </div>
        <mdui-divider></mdui-divider>
        <div class="zf-footer-bottom">
          &copy; 2026 Artzhu · Made with Kotlin &amp; Material You
        </div>
      </div>
    </footer>
      </mdui-layout-main>
    </mdui-layout>

    <mdui-fab id="back-to-top" extended label="回到顶部" variant="primary">
      <mdui-icon slot="icon" name="arrow_upward"></mdui-icon>
    </mdui-fab>
  `

  /* ---------- 交互绑定 ---------- */
  // 菜单按钮切换抽屉
  const menuToggle = document.getElementById('menu-toggle')!
  const navDrawer = document.getElementById('nav-drawer') as any
  const MD_BREAKPOINT = 840

  // 宽屏默认打开抽屉(常驻侧栏),窄屏默认关闭(点击菜单打开)
  function syncDrawerWithViewport() {
    navDrawer.open = window.innerWidth >= MD_BREAKPOINT
  }
  syncDrawerWithViewport()

  // 跨越断点时自动切换抽屉状态
  let lastWide = window.innerWidth >= MD_BREAKPOINT
  window.addEventListener('resize', () => {
    const nowWide = window.innerWidth >= MD_BREAKPOINT
    if (nowWide !== lastWide) {
      lastWide = nowWide
      syncDrawerWithViewport()
    }
  })

  menuToggle.addEventListener('click', () => {
    navDrawer.open = !navDrawer.open
  })

  // 主题切换:使用 mdui-theme-dark 类
  const themeToggle = document.getElementById('theme-toggle')!
  const savedTheme = localStorage.getItem('zhufile-theme') || 'light'
  applyTheme(savedTheme === 'dark')
  themeToggle.addEventListener('click', () => {
    const isDark = document.documentElement.classList.contains('mdui-theme-dark')
    applyTheme(!isDark)
    localStorage.setItem('zhufile-theme', !isDark ? 'dark' : 'light')
  })

  function applyTheme(isDark: boolean) {
    document.documentElement.classList.toggle('mdui-theme-dark', isDark)
    themeToggle.setAttribute('icon', isDark ? 'light_mode' : 'dark_mode')
  }

  // 回到顶部按钮
  const backToTop = document.getElementById('back-to-top') as any
  const scrollContainer = document.querySelector('mdui-layout-main') as HTMLElement | null
  backToTop.addEventListener('click', () => {
    if (scrollContainer) {
      scrollContainer.scrollTo({ top: 0, behavior: 'smooth' })
    } else {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  })

  // 滚动监听:控制回顶按钮显隐
  const scrollTarget: HTMLElement | Window = scrollContainer || window
  scrollTarget.addEventListener('scroll', () => {
    const scrollTop = scrollContainer ? scrollContainer.scrollTop : window.scrollY
    if (scrollTop > 400) {
      backToTop.classList.add('visible')
    } else {
      backToTop.classList.remove('visible')
    }
  })

  // 全局滚动进入视口动画(IntersectionObserver)
  setupScrollAnimations()
}

/**
 * 滚动进入视口动画
 * 为所有带 animate-* 类的元素添加 in-view 类触发动画
 */
function setupScrollAnimations() {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('in-view')
        observer.unobserve(entry.target)
      }
    })
  }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' })

  // 延迟一帧确保页面内容已渲染
  requestAnimationFrame(() => {
    document.querySelectorAll(
      '.animate-in, .animate-fade, .animate-scale, .animate-left, .animate-right'
    ).forEach(el => observer.observe(el))
  })
}
