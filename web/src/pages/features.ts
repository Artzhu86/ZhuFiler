import { initLayout } from '../layout'
import { copyWithFeedback, getElementText } from '../lib/clipboard'

initLayout('features')

/* ---------- 特性数据(消除重复 HTML) ---------- */
interface FeatureItem {
  icon: string
  title: string
  subtitle: string
  variant: 'filled' | 'elevated'
  delay: string
  code: string
  content: 'list' | 'desc-chips' | 'desc'
  bullets?: { name: string; desc: string }[]
  chips?: string[]
  desc?: string
  desc2?: string
}

const FEATURES: FeatureItem[] = [
  {
    icon: 'folder_open',
    title: '文件浏览',
    subtitle: '告别繁琐的文件查找',
    variant: 'filled',
    delay: '',
    content: 'list',
    bullets: [
      { name: '路径导航', desc: '工具栏显示当前路径，点击可直接输入' },
      { name: '快速滚动', desc: '内置快速滚动条，长列表也能快速定位' },
      { name: '双指缩放', desc: '支持网格视图，双指缩放调整图标大小' },
      { name: '下拉刷新', desc: '重命名或新建文件后，下拉即可刷新' },
      { name: '记忆位置', desc: '记住每个目录的滚动位置，下次打开自动恢复' },
    ],
    code: `<span class="tk-key">class</span> <span class="tk-cls">FileBrowserController</span> {
    <span class="tk-key">fun</span> <span class="tk-fn">loadDirectory</span>(path: <span class="tk-cls">String</span>) {
        <span class="tk-cmt">// 1. 加载文件列表</span>
        <span class="tk-cmt">// 2. 应用过滤和排序</span>
        <span class="tk-cmt">// 3. 恢复滚动位置</span>
        <span class="tk-cmt">// 4. 高亮新文件</span>
    }
}`,
  },
  {
    icon: 'description',
    title: '文件预览',
    subtitle: '内置预览，无需跳转',
    variant: 'elevated',
    delay: 'delay-1',
    content: 'desc-chips',
    desc: '<strong>文本/代码查看器</strong><br>支持 13 种编程语言的语法高亮,基于 TextMate 语法,专业级高亮效果。支持暗色/亮色主题切换。',
    chips: ['Kotlin', 'Java', 'JavaScript', 'Python', 'C/C++', 'HTML', 'CSS', 'JSON', 'XML', 'Markdown', 'Lua'],
    code: `<span class="tk-key">object</span> <span class="tk-cls">CodeEditorTextMate</span> {
    <span class="tk-key">private val</span> languageMap = <span class="tk-fn">mapOf</span>(
        <span class="tk-str">"kt"</span> <span class="tk-key">to</span> <span class="tk-str">"source.kotlin"</span>,
        <span class="tk-str">"java"</span> <span class="tk-key">to</span> <span class="tk-str">"source.java"</span>,
        <span class="tk-str">"py"</span> <span class="tk-key">to</span> <span class="tk-str">"source.python"</span>,
        <span class="tk-str">"js"</span> <span class="tk-key">to</span> <span class="tk-str">"source.js"</span>,
        <span class="tk-cmt">// 13 种语言:c, cpp, css, html, java, js,</span>
        <span class="tk-cmt">// json, jsonc, kotlin, lua, md, python, xml</span>
    )
}`,
  },
  {
    icon: 'image',
    title: '图片查看',
    subtitle: '像专业相册应用一样看图',
    variant: 'filled',
    delay: 'delay-2',
    content: 'list',
    bullets: [
      { name: '双指缩放', desc: '基于 PhotoView，缩放体验丝滑' },
      { name: '全屏模式', desc: '沉浸式查看，无干扰' },
      { name: '手势操作', desc: '滑动切换，双击放大' },
      { name: '快速预览', desc: '点击即可打开，无需等待' },
    ],
    code: `<span class="tk-key">class</span> <span class="tk-cls">ImagePreviewActivity</span> : AppCompatActivity() {
    <span class="tk-key">private lateinit var</span> photoView: <span class="tk-cls">PhotoView</span>

    <span class="tk-key">override fun</span> <span class="tk-fn">onCreate</span>(savedInstanceState: Bundle?) {
        <span class="tk-cmt">// 加载图片</span>
        <span class="tk-cmt">// 配置缩放</span>
        <span class="tk-cmt">// 设置全屏</span>
    }
}`,
  },
  {
    icon: 'search',
    title: '文件搜索',
    subtitle: '找文件？就是这么快',
    variant: 'elevated',
    delay: 'delay-3',
    content: 'list',
    bullets: [
      { name: '实时搜索', desc: '输入即搜索，无需等待' },
      { name: '递归搜索', desc: '可选搜索子目录' },
      { name: '结果导航', desc: '点击搜索结果直接跳转' },
      { name: '取消搜索', desc: '随时可取消，不卡顿' },
    ],
    code: `<span class="tk-key">class</span> <span class="tk-cls">FindHelper</span>(<span class="tk-key">private val</span> context: Context) {
    <span class="tk-key">suspend fun</span> <span class="tk-fn">searchFiles</span>(
        query: String,
        path: String,
        recursive: Boolean = <span class="tk-key">false</span>
    ): List&lt;File&gt; = withContext(Dispatchers.IO) {
        <span class="tk-cmt">// 1. 遍历目录</span>
        <span class="tk-cmt">// 2. 匹配文件名</span>
        <span class="tk-cmt">// 3. 支持取消</span>
        <span class="tk-cmt">// 4. 返回结果</span>
    }
}`,
  },
  {
    icon: 'bookmark',
    title: '书签系统',
    subtitle: '常用目录，一键直达',
    variant: 'filled',
    delay: 'delay-4',
    content: 'list',
    bullets: [
      { name: '默认书签', desc: '内置 DCIM、Downloads、Pictures' },
      { name: '自定义书签', desc: '长按添加任意目录' },
      { name: '抽屉导航', desc: '侧边栏快速访问' },
      { name: '固定项目', desc: 'Root 和内部存储始终显示' },
    ],
    code: `<span class="tk-key">class</span> <span class="tk-cls">BookmarkManager</span>(<span class="tk-key">private val</span> context: Context) {
    <span class="tk-key">fun</span> <span class="tk-fn">addBookmark</span>(path: String, name: String) {
        <span class="tk-cmt">// 1. 验证路径有效</span>
        <span class="tk-cmt">// 2. 存储到 SharedPreferences</span>
        <span class="tk-cmt">// 3. 更新 UI</span>
    }

    <span class="tk-key">fun</span> <span class="tk-fn">getBookmarks</span>(): List&lt;Bookmark&gt; {
        <span class="tk-cmt">// 返回用户书签列表</span>
    }
}`,
  },
  {
    icon: 'select_all',
    title: '多选操作',
    subtitle: '批量操作，效率翻倍',
    variant: 'elevated',
    delay: 'delay-5',
    content: 'list',
    bullets: [
      { name: '手势选中', desc: '左滑选中，右滑取消' },
      { name: '全选/取消', desc: '一键全选或取消' },
      { name: '批量操作', desc: '复制、移动、删除、分享' },
      { name: '触觉反馈', desc: '选中时有轻微震动反馈' },
    ],
    code: `<span class="tk-key">class</span> <span class="tk-cls">MultiSelectController</span> {
    <span class="tk-key">fun</span> <span class="tk-fn">toggleSelection</span>(position: Int) {
        <span class="tk-cmt">// 切换选中状态</span>
        <span class="tk-cmt">// 提供触觉反馈</span>
        <span class="tk-cmt">// 更新工具栏</span>
    }

    <span class="tk-key">fun</span> <span class="tk-fn">batchDelete</span>(files: List&lt;File&gt;) {
        <span class="tk-cmt">// 批量删除</span>
        <span class="tk-cmt">// 显示进度</span>
        <span class="tk-cmt">// 支持取消</span>
    }
}`,
  },
]

const MINI_FEATURES = [
  { icon: 'account_tree', title: '目录树导航', desc: '快速跳转到任意层级' },
  { icon: 'history', title: '最近目录', desc: '记住最近10个目录' },
  { icon: 'share', title: '分享功能', desc: '通过系统分享发送文件' },
  { icon: 'info', title: '文件属性', desc: '查看文件详细信息' },
  { icon: 'drive_file_rename_outline', title: '重命名', desc: '单个和批量重命名' },
  { icon: 'note_add', title: '创建文件/目录', desc: '快速创建新文件或文件夹' },
]

/* ---------- 渲染 ---------- */
function renderBullets(items: { name: string; desc: string }[]) {
  return `<ul class="zf-list">
    ${items
      .map(
        (b) => `<li class="zf-check-item-li">
        <mdui-icon name="check"></mdui-icon>
        <span><strong>${b.name}</strong> - ${b.desc}</span>
      </li>`
      )
      .join('')}
  </ul>`
}

function renderContent(f: FeatureItem) {
  if (f.content === 'list') return renderBullets(f.bullets!)
  if (f.content === 'desc-chips') {
    return `
      <p class="zf-feature-block-desc">${f.desc}</p>
      <div class="zf-chip-row zf-chip-row--inline">
        ${f.chips!.map((c) => `<mdui-chip variant="tonal">${c}</mdui-chip>`).join('')}
      </div>
    `
  }
  return ''
}

const main = document.getElementById('main-content')!
main.innerHTML = `
  <div class="page-header">
    <div class="container">
      <mdui-icon class="page-header-icon animate-scale" name="star"></mdui-icon>
      <h1 class="page-title animate-in delay-1">功能特性</h1>
      <p class="page-desc animate-in delay-2">不只是文件管理器。我们重新思考了文件管理应该是什么样子。</p>
    </div>
  </div>

  <div class="section">
    <div class="container zf-content-narrow">
      ${FEATURES.map(
        (f, i) => `
        <mdui-card class="zf-card animate-in ${f.delay}" variant="${f.variant}">
          <div class="zf-feature-2col">
            <div>
              <div class="zf-feature-block">
                <mdui-icon name="${f.icon}"></mdui-icon>
                <h2>${f.title}</h2>
              </div>
              <h3 class="zf-feature-block-text">${f.subtitle}</h3>
              ${renderContent(f)}
            </div>
            <div class="code-block-wrap">
              <mdui-button-icon class="code-copy-btn" data-copy-target="feat-${i}" icon="content_copy" variant="standard" aria-label="复制"></mdui-button-icon>
              <pre class="code-block" id="feat-${i}"><code>${f.code}</code></pre>
            </div>
          </div>
        </mdui-card>
      `
      ).join('')}

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="more_horiz"></mdui-icon>
        更多功能
      </h2>

      <div class="grid grid-auto-sm">
        ${MINI_FEATURES.map(
          (m) => `
          <mdui-card class="mini-card animate-in" variant="outlined">
            <mdui-icon name="${m.icon}"></mdui-icon>
            <div class="mini-card-title">${m.title}</div>
            <div class="mini-card-desc">${m.desc}</div>
          </mdui-card>
        `
        ).join('')}
      </div>

      <div class="cta-section animate-scale">
        <h2>亲自体验</h2>
        <p>这些功能听起来不错？不如自己试试。</p>
        <div class="cta-actions">
          <mdui-button href="download.html" icon="download" variant="filled">下载 ZhuFiler</mdui-button>
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
    copyWithFeedback(text, '已复制代码到剪贴板', '复制失败')
  })
})
