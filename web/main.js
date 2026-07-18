mdui.setColorScheme('#1976D2')

const app = document.getElementById('app')
app.innerHTML = `
  <mdui-layout full-height>
    <mdui-top-app-bar id="top-bar" variant="small" style="padding-left:16px;padding-right:16px">
      <mdui-top-app-bar-title>烛文件</mdui-top-app-bar-title>
      <div style="flex-grow:1"></div>
      <mdui-button-icon icon="dark_mode" id="theme-toggle"></mdui-button-icon>
    </mdui-top-app-bar>

    <mdui-layout-main class="zf-layout-main">
      <div id="main-content"></div>
      <footer class="zf-footer">
        <div class="zf-footer-inner">
          <div style="font-size:var(--mdui-typescale-body-large-size);font-weight:var(--mdui-typescale-body-large-weight);line-height:var(--mdui-typescale-body-large-line-height);color:rgb(var(--mdui-color-on-surface))">&copy; 2026 烛光</div>
        </div>
      </footer>
    </mdui-layout-main>
  </mdui-layout>
`

document.getElementById('main-content').innerHTML = `
  <section class="hero">
    <div class="hero-bg"></div>
    <div class="container">
      <div class="hero-content" style="max-width:680px;margin:0 auto;text-align:center">
        <img src="icon.png" alt="烛文件" class="animate-scale" style="width:128px;height:128px;border-radius:28px;margin:0 auto 24px;display:block;box-shadow:var(--zf-shadow-float)">
        <h1 class="hero-title animate-in">烛文件</h1>
        <p class="hero-desc animate-in delay-1">Kotlin 编写的开源 Android 文件管理器<br>开源免费，美观强大</p>
        <div class="hero-actions animate-in delay-3" style="justify-content:center">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler/releases/latest" target="_blank" rel="noopener" icon="download" variant="filled">下载 APK</mdui-button>
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener" variant="outlined">GitHub</mdui-button>
        </div>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="container">
      <div class="grid grid-auto">
        <mdui-card class="feature-card animate-in delay-1" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="verified"></mdui-icon>
          <h3>开源</h3>
          <p>MIT 协议，代码透明可审计，接受社区贡献</p>
        </mdui-card>
        <mdui-card class="feature-card animate-in delay-2" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="payment"></mdui-icon>
          <h3>免费</h3>
          <p>无广告、无内购，所有功能永久免费无限制</p>
        </mdui-card>
        <mdui-card class="feature-card animate-in delay-3" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="palette"></mdui-icon>
          <h3>美观</h3>
          <p>Material You 动态主题，支持深色模式与多套配色</p>
        </mdui-card>
        <mdui-card class="feature-card animate-in delay-4" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="extension"></mdui-icon>
          <h3>强大</h3>
          <p>集成解压、编辑、播放、APK，以及 Shizuku 支持</p>
        </mdui-card>
      </div>
    </div>
  </section>
`

const themeToggle = document.getElementById('theme-toggle')
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
const savedTheme = localStorage.getItem('zhufile-theme')
const isDark = savedTheme ? savedTheme === 'dark' : prefersDark
applyTheme(isDark)
themeToggle.addEventListener('click', () => {
  const dark = !document.documentElement.classList.contains('mdui-theme-dark')
  applyTheme(dark)
  localStorage.setItem('zhufile-theme', dark ? 'dark' : 'light')
})
function applyTheme(dark) {
  document.documentElement.classList.toggle('mdui-theme-dark', dark)
  themeToggle.setAttribute('icon', dark ? 'light_mode' : 'dark_mode')
}

const topBar = document.getElementById('top-bar')
const layoutMain = document.querySelector('.zf-layout-main')
function updateTopBar() {
  topBar.classList.toggle('scrolled', layoutMain.scrollTop > 8)
}
layoutMain.addEventListener('scroll', updateTopBar, { passive: true })
updateTopBar()

const observer = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      entry.target.classList.add('in-view')
      observer.unobserve(entry.target)
    }
  })
}, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' })
requestAnimationFrame(() => {
  document.querySelectorAll('.animate-in, .animate-scale').forEach(el => observer.observe(el))
})
