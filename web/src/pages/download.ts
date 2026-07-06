import { initLayout } from '../layout'
import { copyWithFeedback, getElementText } from '../lib/clipboard'
import { getRecentReleases } from '../lib/releases'

initLayout('download')

/* ---------- 数据(消除重复 HTML) ---------- */
const REQS = [
  { icon: 'android', title: 'Android 版本', value: 'Android 7.0+', sub: '支持从 Android 7 到 Android 15' },
  { icon: 'memory', title: '存储空间', value: '约 7 MB', sub: '应用本身只有几MB' },
  { icon: 'security', title: '权限', value: '存储权限', sub: '用于访问和管理你的文件' },
  { icon: 'smartphone', title: '设备', value: '手机或平板', sub: '支持所有主流厂商' },
]

/* 版本历史只显示最近 2 版(避免与 changelog 页重复),完整历史见 changelog.html */
const RECENT_RELEASES = getRecentReleases(2)

const FAQS = [
  {
    q: '下载后如何安装？',
    a: `<ol>
      <li>下载 APK 文件</li>
      <li>在手机设置中开启"允许安装未知来源应用"</li>
      <li>找到下载的 APK 文件并点击安装</li>
      <li>按照提示完成安装</li>
    </ol>`,
  },
  {
    q: '为什么需要存储权限？',
    a: '<p>ZhuFiler 是一个文件管理器，需要访问你的文件系统才能正常工作。我们承诺不会读取或上传你的任何文件。</p>',
  },
  {
    q: '支持哪些文件格式？',
    a: `<p>ZhuFiler 支持所有常见的文件格式：</p>
      <ul>
        <li><strong>文本文件</strong>: .txt, .md, .json, .xml, .html, .css, .js 等</li>
        <li><strong>代码文件</strong>: .kt, .java, .py, .c, .cpp, .lua 等</li>
        <li><strong>图片文件</strong>: .jpg, .png, .gif, .webp 等</li>
        <li><strong>其他文件</strong>: .pdf, .doc, .zip 等（使用外部应用打开）</li>
      </ul>`,
  },
  {
    q: '如何报告问题或建议功能？',
    a: `<p>你可以通过以下方式联系我们：</p>
      <ol>
        <li>在 GitHub 上提交 <a href="https://github.com/Artzhu86/ZhuFiler/issues" target="_blank">Issue</a></li>
        <li>发送邮件到项目维护者</li>
        <li>在 GitHub Discussions 中讨论</li>
      </ol>`,
  },
]

const main = document.getElementById('main-content')!
main.innerHTML = `
  <div class="page-header">
    <div class="container">
      <mdui-icon class="page-header-icon animate-scale" name="download"></mdui-icon>
      <h1 class="page-title animate-in delay-1">下载 ZhuFiler</h1>
      <p class="page-desc animate-in delay-2">最新稳定版，免费下载，支持 Android 7.0+</p>
    </div>
  </div>

  <div class="section">
    <div class="container-narrow">

      <div class="zf-version-badge animate-in">v1.9 · 2026年7月4日发布</div>

      <mdui-card class="zf-card animate-in delay-1" variant="filled">
        <div class="zf-button-row">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler/releases/latest" target="_blank" icon="download" variant="filled">下载 APK</mdui-button>
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler/releases" target="_blank" icon="open_in_new" variant="outlined">GitHub Releases</mdui-button>
        </div>
        <div class="zf-meta">文件大小: ~3MB · 支持 Android 7.0+</div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="terminal"></mdui-icon>
        命令行下载
      </h2>

      <div class="zf-cmd-list animate-in delay-1 zf-block-space">
        <div class="zf-cmd-row">
          <mdui-button-icon class="code-copy-btn" data-copy-target="cmd-1" icon="content_copy" variant="standard" aria-label="复制"></mdui-button-icon>
          <div class="zf-cmd-content">
            <div class="zf-cmd-label">下载 APK</div>
            <pre class="zf-cmd-code" id="cmd-1"><code>curl -LO https://github.com/Artzhu86/ZhuFiler/releases/latest/download/ZhuFiler.apk</code></pre>
          </div>
        </div>
        <div class="zf-cmd-row">
          <mdui-button-icon class="code-copy-btn" data-copy-target="cmd-2" icon="content_copy" variant="standard" aria-label="复制"></mdui-button-icon>
          <div class="zf-cmd-content">
            <div class="zf-cmd-label">安装到设备</div>
            <pre class="zf-cmd-code" id="cmd-2"><code>adb install ZhuFiler.apk</code></pre>
          </div>
        </div>
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="checklist"></mdui-icon>
        系统要求
      </h2>

      <div class="grid grid-2 animate-in delay-1 zf-block-space">
        ${REQS.map(
          (r) => `
          <mdui-card class="zf-card-sm zf-card-accent" variant="elevated">
            <div class="zf-card-head">
              <mdui-icon name="${r.icon}"></mdui-icon>
              <span>${r.title}</span>
            </div>
            <div class="zf-emph-value">${r.value}</div>
            <div class="zf-emph-sub">${r.sub}</div>
          </mdui-card>
        `
        ).join('')}
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="history"></mdui-icon>
        最近更新
      </h2>

      <div class="zf-timeline">
        ${RECENT_RELEASES.map(
          (r, i) => `
          <div class="zf-timeline-item animate-in delay-${i + 1}">
            <div class="zf-timeline-date">${r.date}</div>
            <ul class="zf-list">
              ${r.groups
                .flatMap((g) => g.items)
                .map(
                  (item) => `<li class="zf-check-item-sm">
                  <mdui-icon name="check"></mdui-icon>
                  <span>${item}</span>
                </li>`
                )
                .join('')}
            </ul>
          </div>
        `
        ).join('')}
      </div>

      <div class="zf-block-space animate-in">
        <mdui-button href="changelog.html" icon="arrow_forward" variant="outlined">查看完整更新历史</mdui-button>
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="help"></mdui-icon>
        常见问题
      </h2>

      <mdui-list class="animate-in delay-1 zf-block-space">
        <mdui-collapse accordion>
          ${FAQS.map(
            (f) => `
            <mdui-collapse-item>
              <mdui-list-item slot="header" icon="help">${f.q}</mdui-list-item>
              <div class="mdui-prose zf-faq-content">${f.a}</div>
            </mdui-collapse-item>
          `
          ).join('')}
        </mdui-collapse>
      </mdui-list>

      <div class="cta-section animate-scale">
        <h2>喜欢 ZhuFiler？</h2>
        <p>如果这个应用对你有帮助，欢迎在 GitHub 上给我们一个 Star！</p>
        <div class="cta-actions">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler" target="_blank" icon="star" variant="filled">Star on GitHub</mdui-button>
        </div>
      </div>

    </div>
  </div>
`

/* ---------- 复制按钮:绑定所有 .code-copy-btn ---------- */
document.querySelectorAll<HTMLElement>('.code-copy-btn').forEach((btn) => {
  btn.addEventListener('click', () => {
    const targetId = btn.dataset.copyTarget
    if (!targetId) return
    const text = getElementText(targetId)
    if (!text) return
    copyWithFeedback(text, '已复制命令到剪贴板', '复制失败,请手动选择')
  })
})
