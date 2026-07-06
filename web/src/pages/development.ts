import { initLayout } from '../layout'
import { copyWithFeedback, getElementText } from '../lib/clipboard'

initLayout('development')

/* ---------- 数据 ---------- */
const STACK = [
  { icon: 'code', title: 'Kotlin', role: '主要开发语言', list: ['协程支持', '扩展函数', '数据类', 'JVM 17 字节码'] },
  { icon: 'palette', title: 'Material Design 3', role: 'UI 框架', list: ['Material 组件 1.12.0', '现代组件库', 'Edge-to-Edge', 'viewBinding'] },
  { icon: 'edit_note', title: 'Sora Editor', role: '代码编辑器', list: ['Rosemoe Editor 0.23.7', 'TextMate 语言包', '多语言高亮', '暗色主题'] },
  { icon: 'image', title: 'Glide', role: '图片加载库', list: ['Glide 4.16.0', '高效图片缓存', '缩略图生成', '生命周期感知'] },
]

const REQS = [
  { name: 'Android Studio', detail: '较新版本即可' },
  { name: 'JDK', detail: '17' },
  { name: 'Android SDK', detail: 'compileSdk 36 / targetSdk 36' },
  { name: 'minSdk', detail: '25 (Android 7.1)' },
]

const DEPS = [
  { name: 'androidx.core:core-ktx', version: '1.13.1', use: 'Kotlin 扩展' },
  { name: 'androidx.appcompat:appcompat', version: '1.7.0', use: '向后兼容' },
  { name: 'com.google.android.material:material', version: '1.12.0', use: 'UI 组件' },
  { name: 'androidx.constraintlayout:constraintlayout', version: '2.1.4', use: '布局' },
  { name: 'androidx.lifecycle:lifecycle-runtime-ktx', version: '2.8.7', use: '生命周期' },
  { name: 'androidx.swiperefreshlayout:swiperefreshlayout', version: '1.1.0', use: '下拉刷新' },
  { name: 'com.github.bumptech.glide:glide', version: '4.16.0', use: '图片加载' },
  { name: 'io.github.rosemoe:editor-bom', version: '0.23.7', use: 'Sora Editor BOM' },
  { name: 'io.github.rosemoe:editor', version: '0.23.7', use: '代码编辑' },
  { name: 'io.github.rosemoe:language-textmate', version: '0.23.7', use: 'TextMate 语言' },
  { name: 'com.android.tools:desugar_jdk_libs', version: '2.1.5', use: 'JDK 8+ desugaring' },
  { name: 'PhotoView', version: '本地源码', use: '图片缩放' },
]

const STEPS = [
  { num: 1, title: 'Fork', desc: '仓库' },
  { num: 2, title: '创建分支', desc: 'Feature 分支' },
  { num: 3, title: '提交代码', desc: 'Conventional Commits' },
  { num: 4, title: 'PR', desc: 'Pull Request' },
]

const COMMITS = ['feat: 新功能', 'fix: 修复bug', 'docs: 文档更新', 'style: 代码格式', 'refactor: 重构', 'test: 测试', 'chore: 其他']

const PROJECT_STRUCTURE = `ZhuFiler/
├── app/
│   ├── src/main/
│   │   ├── kotlin/zhu/filer/                 <span class="tk-cmt"># ZhuFiler 主代码(全部 Kotlin)</span>
│   │   │   ├── MainActivity.kt               <span class="tk-cmt"># 主界面(单 Activity)</span>
│   │   │   ├── TextPreviewActivity.kt        <span class="tk-cmt"># 文本/代码查看器</span>
│   │   │   ├── ImagePreviewActivity.kt       <span class="tk-cmt"># 图片查看器(PhotoView)</span>
│   │   │   ├── FileBrowserController.kt      <span class="tk-cmt"># 文件浏览控制</span>
│   │   │   ├── FileListAdapter.kt            <span class="tk-cmt"># 文件列表适配器</span>
│   │   │   ├── FileListLoader.kt             <span class="tk-cmt"># 文件加载器</span>
│   │   │   ├── FileUtils.kt                  <span class="tk-cmt"># 工具函数(增删改查)</span>
│   │   │   ├── BookmarkManager.kt            <span class="tk-cmt"># 书签管理</span>
│   │   │   ├── ClipboardManager.kt           <span class="tk-cmt"># 剪贴板(复制/剪切)</span>
│   │   │   ├── MultiSelectController.kt      <span class="tk-cmt"># 多选控制</span>
│   │   │   ├── FindHelper.kt                 <span class="tk-cmt"># 搜索功能</span>
│   │   │   ├── CodeEditorTextMate.kt         <span class="tk-cmt"># TextMate 配置</span>
│   │   │   ├── FabManager.kt                 <span class="tk-cmt"># 浮动按钮</span>
│   │   │   ├── FastScroller.kt               <span class="tk-cmt"># 快速滚动条</span>
│   │   │   ├── SwipeToSelectCallback.kt      <span class="tk-cmt"># 滑动选择手势</span>
│   │   │   ├── BackPressHandler.kt           <span class="tk-cmt"># 返回键处理</span>
│   │   │   ├── AnimationHelper.kt            <span class="tk-cmt"># 动画</span>
│   │   │   └── DialogHelpers.kt              <span class="tk-cmt"># 对话框</span>
│   │   ├── java/com/github/chrisbanes/photoview/   <span class="tk-cmt"># PhotoView 本地源码</span>
│   │   │   ├── PhotoView.java + Attacher     <span class="tk-cmt"># 图片缩放核心</span>
│   │   │   └── OnGestureListener.java ...    <span class="tk-cmt"># 手势监听器</span>
│   │   ├── assets/textmate/                  <span class="tk-cmt"># TextMate 语法定义(13 种语言)</span>
│   │   │   ├── c/, cpp/, css/, html/, java/, javascript/,
│   │   │   ├── json/, kotlin/, lua/, markdown/,
│   │   │   └── python/, xml/                 <span class="tk-cmt"># 语法文件</span>
│   │   ├── res/                              <span class="tk-cmt"># 资源(layout/values/drawable)</span>
│   │   └── AndroidManifest.xml               <span class="tk-cmt"># 权限声明 + FileProvider</span>
│   ├── proguard-rules.pro                    <span class="tk-cmt"># R8 混淆规则</span>
│   └── build.gradle                          <span class="tk-cmt"># 模块构建配置</span>
├── build.gradle                              <span class="tk-cmt"># 项目级构建配置</span>
├── gradle.properties                         <span class="tk-cmt"># JVM/AndroidX 配置</span>
├── artzhu.keystore                           <span class="tk-cmt"># 签名密钥</span>
├── LICENSE                                   <span class="tk-cmt"># MIT License</span>
└── README.md                                 <span class="tk-cmt"># 项目说明</span>`

const BUILD_STEPS = `<span class="tk-cmt"># 克隆仓库</span>
git clone https://github.com/Artzhu86/ZhuFiler.git
cd ZhuFiler

<span class="tk-cmt"># 构建 Debug 版本</span>
./gradlew assembleDebug

<span class="tk-cmt"># 构建 Release 版本</span>
./gradlew assembleRelease

<span class="tk-cmt"># 安装到设备</span>
./gradlew installDebug`

/* ---------- 渲染 ---------- */
const main = document.getElementById('main-content')!
main.innerHTML = `
  <div class="page-header">
    <div class="container">
      <mdui-icon class="page-header-icon animate-scale" name="code"></mdui-icon>
      <h1 class="page-title animate-in delay-1">开发文档</h1>
      <p class="page-desc animate-in delay-2">为开发者准备。如果你想了解 ZhuFiler 的技术细节或参与贡献，这个页面就是为你准备的。</p>
    </div>
  </div>

  <div class="section">
    <div class="container-wide">

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="code"></mdui-icon>
        技术栈
      </h2>

      <div class="grid grid-4 zf-block-space">
        ${STACK.map(
          (s, i) => `
          <mdui-card class="zf-card zf-feature-card animate-scale delay-${i + 1}" variant="elevated">
            <mdui-icon name="${s.icon}"></mdui-icon>
            <h3>${s.title}</h3>
            <div class="zf-feature-role">${s.role}</div>
            <div class="zf-feature-list">${s.list.join('<br>')}</div>
          </mdui-card>
        `
        ).join('')}
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="folder"></mdui-icon>
        项目结构
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 48px; padding: 24px;">
        <div class="code-block"><pre><code>${PROJECT_STRUCTURE}</code></pre></div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="settings"></mdui-icon>
        构建说明
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 16px;">
        <h3 class="zf-subtitle">环境要求</h3>
        <div class="grid grid-2">
          ${REQS.map(
            (r) => `
            <div class="zf-req-item">
              <mdui-icon name="check_circle" class="zf-tone-primary"></mdui-icon>
              <span><strong>${r.name}</strong> ${r.detail}</span>
            </div>
          `
          ).join('')}
        </div>
      </mdui-card>

      <mdui-card class="zf-card animate-in delay-2" variant="filled" style="margin-bottom: 48px;">
        <h3 class="zf-subtitle-with-icon">
          <mdui-icon name="terminal"></mdui-icon>
          构建步骤
        </h3>
        <div class="code-block-wrap">
          <mdui-button-icon class="code-copy-btn" data-copy-target="build-steps" icon="content_copy" variant="standard" aria-label="复制"></mdui-button-icon>
          <pre class="code-block" id="build-steps"><code>${BUILD_STEPS}</code></pre>
        </div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="architecture"></mdui-icon>
        架构设计
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 16px;">
        <div class="mdui-prose">
          <p class="zf-architecture-intro">ZhuFiler 采用单 Activity 架构，所有界面都是独立的 Activity。</p>
          <h3 class="zf-subtitle-sm">数据流</h3>
          <ol class="zf-list-prose">
            <li><strong>用户操作</strong> → MainActivity</li>
            <li><strong>界面更新</strong> → FileListAdapter</li>
            <li><strong>文件加载</strong> → FileListLoader</li>
            <li><strong>文件操作</strong> → FileUtils(增删改查)</li>
            <li><strong>图片预览</strong> → ImagePreviewActivity + PhotoView</li>
            <li><strong>文本预览</strong> → TextPreviewActivity + CodeEditorTextMate</li>
          </ol>
        </div>
      </mdui-card>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="extension"></mdui-icon>
        依赖库
      </h2>

      <div class="zf-table-wrap animate-in delay-1 zf-block-space">
        <table>
          <thead><tr><th>库名</th><th>版本</th><th>用途</th></tr></thead>
          <tbody>
            ${DEPS.map((d) => `<tr><td>${d.name}</td><td>${d.version}</td><td>${d.use}</td></tr>`).join('')}
          </tbody>
        </table>
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="source"></mdui-icon>
        贡献代码
      </h2>

      <mdui-card class="zf-card animate-in delay-1" variant="filled" style="margin-bottom: 16px;">
        <h3 class="zf-subtitle">开发流程</h3>
        <div class="grid grid-4">
          ${STEPS.map(
            (s) => `
            <div class="zf-step">
              <div class="zf-step-num">${s.num}</div>
              <div class="zf-step-title">${s.title}</div>
              <div class="zf-step-desc">${s.desc}</div>
            </div>
          `
          ).join('')}
        </div>
      </mdui-card>

      <mdui-card class="zf-card animate-in delay-2" variant="filled" style="margin-bottom: 48px;">
        <h3 class="zf-subtitle">提交规范</h3>
        <div class="zf-chip-row">
          ${COMMITS.map((c) => `<mdui-chip variant="tonal">${c}</mdui-chip>`).join('')}
        </div>
      </mdui-card>

      <div class="cta-section animate-scale">
        <h2>加入我们</h2>
        <p>ZhuFiler 是一个开源项目，欢迎任何人参与贡献。</p>
        <div class="cta-actions">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler" target="_blank" rel="noopener" icon="fork_right" variant="filled">Fork on GitHub</mdui-button>
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
    copyWithFeedback(text, '已复制命令到剪贴板', '复制失败')
  })
})
