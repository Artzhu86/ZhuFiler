import { initLayout } from '../layout'

initLayout('guide')

/* ---------- 数据(消除重复 HTML) ---------- */
const FILE_GESTURES = [
  { icon: 'swipe', name: '左滑', op: '选中文件', desc: '进入多选模式' },
  { icon: 'swipe', name: '右滑', op: '取消选中', desc: '退出多选模式' },
  { icon: 'touch_app', name: '长按', op: '上下文菜单', desc: '显示操作选项' },
  { icon: 'touch_app', name: '双击', op: '打开文件', desc: '快速打开' },
  { icon: 'swap_vert', name: '下拉', op: '刷新', desc: '刷新当前目录' },
]

const TOOLBAR_GESTURES = [
  { icon: 'touch_app', name: '点击路径', op: '跳转', desc: '直接输入路径' },
  { icon: 'touch_app', name: '点击标题', op: '搜索', desc: '开始搜索文件' },
]

const BOOKMARK_OPS = [
  { title: '添加书签', items: ['长按你想收藏的目录', '选择"添加书签"', '书签会出现在侧边栏'] },
  { title: '使用书签', items: ['点击菜单按钮打开侧边栏', '点击任意书签即可跳转', '默认:DCIM、Downloads、Pictures'] },
  { title: '删除书签', items: ['打开侧边栏', '长按要删除的书签', '确认删除'] },
]

const FILE_OPS = [
  { icon: 'check_circle', title: '1. 选择文件', desc: '左滑选择单个文件<br>或长按后选择多个' },
  { icon: 'content_copy', title: '2. 执行操作', desc: '点击"复制"或"剪切"<br>文件添加到剪贴板' },
  { icon: 'content_paste', title: '3. 粘贴文件', desc: '导航到目标目录<br>点击浮动按钮粘贴' },
]

const FAQS = [
  {
    q: '文件打不开怎么办?',
    a: `如果文件无法在 ZhuFiler 中预览,可以尝试:
      <ol>
        <li>长按文件选择"用其他应用打开"</li>
        <li>选择合适的应用</li>
        <li>如果没有合适的应用,可以从应用商店安装</li>
      </ol>`,
  },
  {
    q: '如何访问系统文件?',
    a: `访问系统文件需要 root 权限:
      <ol>
        <li>确保你的设备已 root</li>
        <li>在侧边栏选择"Root"</li>
        <li>授予 root 权限</li>
        <li>小心操作,避免误删系统文件</li>
      </ol>`,
  },
  {
    q: '为什么有些文件显示为未知类型?',
    a: 'ZhuFiler 会根据文件扩展名判断类型。如果文件没有扩展名或扩展名不标准,会显示为通用文件图标。',
  },
  {
    q: '如何恢复误删的文件?',
    a: 'ZhuFiler 的删除操作会将文件移到回收站(如果系统支持)。你可以在系统文件管理器中检查回收站。',
  },
]

/* ---------- 渲染 ---------- */
function renderGestureTable(items: typeof FILE_GESTURES) {
  return `
    <div class="zf-table-wrap">
      <table>
        <thead><tr><th>手势</th><th>操作</th><th>说明</th></tr></thead>
        <tbody>
          ${items
            .map(
              (g) => `<tr>
              <td><mdui-icon name="${g.icon}"></mdui-icon> ${g.name}</td>
              <td>${g.op}</td>
              <td>${g.desc}</td>
            </tr>`
            )
            .join('')}
        </tbody>
      </table>
    </div>
  `
}

const main = document.getElementById('main-content')!
main.innerHTML = `
  <div class="page-header">
    <div class="container">
      <mdui-icon class="page-header-icon animate-scale" name="menu_book"></mdui-icon>
      <h1 class="page-title animate-in delay-1">使用指南</h1>
      <p class="page-desc animate-in delay-2">ZhuFiler 设计简洁,上手容易。不过,这里有一些小技巧能让你用得更顺手。</p>
    </div>
  </div>

  <div class="section">
    <div class="container-wide">

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="rocket_launch"></mdui-icon>
        首次使用
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 24px;">
        <h3 class="zf-subtitle">1. 安装应用</h3>
        <ol class="zf-list-prose mdui-prose">
          <li>从 <a href="download.html">下载页面</a> 获取 APK</li>
          <li>点击 APK 文件开始安装</li>
          <li>允许"安装未知来源应用"</li>
          <li>安装完成后打开 ZhuFiler</li>
        </ol>
      </mdui-card>

      <mdui-card class="zf-card animate-in delay-2" variant="filled" style="margin-bottom: 24px;">
        <h3 class="zf-subtitle">2. 授予权限</h3>
        <p class="zf-meta-text">首次打开时,ZhuFiler 会请求存储权限:</p>
        <mdui-tabs value="android11" variant="secondary">
          <mdui-tab value="android11">Android 11+</mdui-tab>
          <mdui-tab value="android10">Android 10 及以下</mdui-tab>
          <mdui-tab-panel slot="panel" value="android11">
            <div class="mdui-prose zf-tab-prose">
              <ol>
                <li>点击"允许"按钮</li>
                <li>在系统设置中找到"所有文件访问"</li>
                <li>找到 ZhuFiler 并开启权限</li>
                <li>返回应用即可使用</li>
              </ol>
            </div>
          </mdui-tab-panel>
          <mdui-tab-panel slot="panel" value="android10">
            <div class="mdui-prose zf-tab-prose">
              <ol>
                <li>点击"允许"按钮</li>
                <li>确认授予存储权限</li>
                <li>开始使用</li>
              </ol>
            </div>
          </mdui-tab-panel>
        </mdui-tabs>
      </mdui-card>

      <mdui-card class="zf-card animate-in delay-3" variant="filled" style="margin-bottom: 48px;">
        <h3 class="zf-subtitle">3. 开始浏览</h3>
        <div class="mdui-prose zf-tab-prose">
          <p>权限授予后,你会看到:</p>
          <ul>
            <li><strong>内部存储</strong> — 你的手机存储空间</li>
            <li><strong>Root</strong> — 系统根目录(需要 root 权限)</li>
            <li><strong>书签</strong> — 常用目录快捷方式</li>
          </ul>
          <p>点击任意目录即可进入,点击 <code>..</code> 可以返回上级目录。</p>
        </div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="swipe"></mdui-icon>
        手势操作
      </h2>

      <p class="zf-meta-text zf-block-space-sm animate-in">ZhuFiler 支持多种手势操作,让你的操作更高效。</p>

      <h3 class="zf-subtitle animate-in">文件列表手势</h3>
      <div class="animate-in delay-1">${renderGestureTable(FILE_GESTURES)}</div>

      <h3 class="zf-subtitle zf-subtitle-spaced animate-in">工具栏手势</h3>
      <div class="animate-in delay-1">${renderGestureTable(TOOLBAR_GESTURES)}</div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="search"></mdui-icon>
        搜索文件
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 32px;">
        <h3 class="zf-subtitle">基本搜索</h3>
        <ol class="zf-list-prose">
          <li>点击工具栏的搜索图标</li>
          <li>输入文件名或部分名称</li>
          <li>实时显示搜索结果</li>
          <li>点击结果直接跳转</li>
        </ol>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="bookmark"></mdui-icon>
        书签管理
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 48px;">
        <div class="grid grid-3">
          ${BOOKMARK_OPS.map(
            (b) => `
            <div>
              <h3 class="zf-subtitle-sm">${b.title}</h3>
              <ol class="zf-list-prose-sm">
                ${b.items.map((it) => `<li>${it}</li>`).join('')}
              </ol>
            </div>
          `
          ).join('')}
        </div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="content_copy"></mdui-icon>
        文件操作
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 32px;">
        <h3 class="zf-subtitle">复制/剪切文件</h3>
        <div class="grid grid-3">
          ${FILE_OPS.map(
            (f) => `
            <div class="zf-step-card">
              <mdui-icon name="${f.icon}"></mdui-icon>
              <div class="zf-step-card-title">${f.title}</div>
              <div class="zf-step-card-desc">${f.desc}</div>
            </div>
          `
          ).join('')}
        </div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="help"></mdui-icon>
        常见问题
      </h2>

      <mdui-list>
        <mdui-collapse accordion style="margin-bottom: 48px;">
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
        <h2>还有问题?</h2>
        <p>如果你在使用过程中遇到其他问题</p>
        <div class="cta-actions">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler/issues" target="_blank" rel="noopener" icon="bug_report" variant="filled">提交 Issue</mdui-button>
        </div>
      </div>
    </div>
  </div>
`
