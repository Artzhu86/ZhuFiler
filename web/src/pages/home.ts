import { initLayout } from '../layout'
import { fetchRepoStats, animateNumber, setStatsFallback } from '../lib/github'

initLayout('home')

const main = document.getElementById('main-content')!
main.innerHTML = `
  <section class="hero">
    <div class="hero-bg"></div>
    <div class="container hero-grid">
      <div class="hero-content">
        <span class="hero-eyebrow animate-fade">
          <mdui-icon name="bolt"></mdui-icon>
          开源 · 免费 · MIT 协议 · v1.9
        </span>
        <h1 class="hero-title animate-in">烛文件</h1>
        <p class="hero-subtitle animate-in delay-1">
          一个让你重新爱上文件管理的<br>
          <span class="hero-highlight">Android 文件管理器</span>
        </p>
        <p class="hero-desc animate-in delay-2">
          不是那种老古董。动态配色、代码高亮、手势操作、智能书签——<br>
          这才是 2026 年文件管理器该有的样子。
        </p>
        <div class="hero-actions animate-in delay-3">
          <mdui-button href="download.html" icon="download" variant="filled">立即下载</mdui-button>
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener" icon="code" variant="outlined">查看源码</mdui-button>
        </div>
        <div class="hero-stats animate-in delay-4">
          <div>
            <div class="hero-stat-num" id="stars-count">--</div>
            <div class="hero-stat-label">GitHub Stars</div>
          </div>
          <div>
            <div class="hero-stat-num" id="forks-count">--</div>
            <div class="hero-stat-label">Forks</div>
          </div>
          <div>
            <div class="hero-stat-num" id="watchers-count">--</div>
            <div class="hero-stat-label">Watchers</div>
          </div>
        </div>
      </div>
      <div class="hero-code animate-right delay-2">
        <div class="code-window">
          <div class="code-window-bar">
            <span class="code-window-dot code-window-dot--red"></span>
            <span class="code-window-dot code-window-dot--yellow"></span>
            <span class="code-window-dot code-window-dot--green"></span>
            <span class="code-window-title">ZhuFiler.kt</span>
          </div>
          <pre class="code-window-body"><code><span class="tk-key">fun</span> <span class="tk-fn">main</span>() {
    <span class="tk-key">val</span> app = <span class="tk-cls">ZhuFiler</span>().<span class="tk-fn">apply</span> {
        name = <span class="tk-str">"烛文件"</span>
        mission = <span class="tk-str">"让文件管理变得有趣"</span>
        features = <span class="tk-fn">listOf</span>(
            <span class="tk-str">"Material You 设计"</span>,
            <span class="tk-str">"代码高亮查看"</span>,
            <span class="tk-str">"手势操作"</span>,
            <span class="tk-str">"书签系统"</span>
        )
        status = <span class="tk-cls">Status</span>.<span class="tk-cls">READY</span>
    }
    app.<span class="tk-fn">launch</span>()
}</code></pre>
        </div>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="container">
      <h2 class="section-title animate-in">为什么选择 ZhuFiler？</h2>
      <p class="section-subtitle animate-in delay-1">不只是文件管理器,更是你手机里的效率工具</p>
      <div class="grid grid-auto">
        <mdui-card class="feature-card animate-in delay-1" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="palette"></mdui-icon>
          <h3>Material You 风格</h3>
          <p>动态配色、圆润卡片、流畅动画——这才是 2026 年该有的样子。</p>
        </mdui-card>
        <mdui-card class="feature-card animate-in delay-2" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="code"></mdui-icon>
          <h3>代码高亮查看</h3>
          <p>支持 13 种编程语言的语法高亮。在手机上也能优雅地看代码。</p>
        </mdui-card>
        <mdui-card class="feature-card animate-in delay-3" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="swipe"></mdui-icon>
          <h3>手势操作</h3>
          <p>左滑选中、右滑返回、长按操作。文件管理也可以很"丝滑"。</p>
        </mdui-card>
        <mdui-card class="feature-card animate-in delay-4" variant="filled" clickable>
          <mdui-icon class="feature-card-icon" name="bookmark"></mdui-icon>
          <h3>智能书签</h3>
          <p>常用目录一键收藏,再也不用层层翻找。效率?这就是效率。</p>
        </mdui-card>
      </div>
    </div>
  </section>

  <section class="section section-alt">
    <div class="container">
      <h2 class="section-title animate-in">项目数据</h2>
      <p class="section-subtitle animate-in delay-1">透明、开源、值得信赖</p>
      <div class="grid grid-auto-sm">
        <mdui-card class="stat-card animate-scale delay-1" variant="elevated">
          <mdui-icon name="schedule"></mdui-icon>
          <div class="stat-value">v1.9</div>
          <div class="stat-label">最新版本 · 2026年7月4日</div>
          <mdui-button href="download.html" variant="tonal" class="stat-card-btn">下载</mdui-button>
        </mdui-card>
        <mdui-card class="stat-card animate-scale delay-2" variant="elevated">
          <mdui-icon name="code"></mdui-icon>
          <div class="stat-value stat-value-sm">Kotlin + M3</div>
          <div class="stat-label">纯 Kotlin 编写,现代架构</div>
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener" variant="tonal" class="stat-card-btn">源码</mdui-button>
        </mdui-card>
        <mdui-card class="stat-card animate-scale delay-3" variant="elevated">
          <mdui-icon name="description"></mdui-icon>
          <div class="stat-value stat-value-sm">MIT License</div>
          <div class="stat-label">自由使用,自由分享</div>
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler/blob/main/LICENSE" target="_blank" rel="noopener" variant="tonal" class="stat-card-btn">查看</mdui-button>
        </mdui-card>
        <mdui-card class="stat-card animate-scale delay-4" variant="elevated">
          <mdui-icon name="android"></mdui-icon>
          <div class="stat-value">7.0+</div>
          <div class="stat-label">Android 最低版本要求</div>
          <mdui-button href="features.html" variant="tonal" class="stat-card-btn">功能</mdui-button>
        </mdui-card>
      </div>
    </div>
  </section>

  <section class="section">
    <div class="container">
      <div class="cta-section animate-scale">
        <h2>准备好体验了吗?</h2>
        <p>免费下载 · MIT 协议 · 持续更新</p>
        <div class="cta-actions">
          <mdui-button href="download.html" icon="download" variant="filled">下载 v1.9</mdui-button>
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener" icon="star" variant="outlined">Star on GitHub</mdui-button>
        </div>
      </div>
    </div>
  </section>
`

/* ---------- GitHub 数据加载 + 数字滚动 ---------- */
async function loadGitHubStats() {
  const stats = await fetchRepoStats()
  if (!stats) {
    setStatsFallback(['stars-count', 'forks-count', 'watchers-count'])
    return
  }
  animateNumber('stars-count', stats.stars)
  animateNumber('forks-count', stats.forks)
  animateNumber('watchers-count', stats.watchers)
}
loadGitHubStats()
