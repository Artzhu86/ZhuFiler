import { initLayout } from '../layout'
import { getLatestRelease, getOlderReleases, PLANS, type ChangeGroup } from '../lib/releases'

initLayout('changelog')

/* 数据来源:共享模块 lib/releases.ts(与 download 页保持一致) */

const COMPARE = [
  { feat: '文件浏览', cells: [true, true, true, true] },
  { feat: '文件操作', cells: [true, true, true, true] },
  { feat: '代码高亮', cells: [false, true, true, true] },
  { feat: '图片预览', cells: [false, true, true, true] },
  { feat: '文件搜索', cells: [false, false, true, true] },
  { feat: '书签系统', cells: [false, false, true, true] },
  { feat: '多选操作', cells: [false, false, true, true] },
]

/* ---------- 渲染 ---------- */
function renderGroup(g: ChangeGroup) {
  return `
    <div class="zf-change-group">
      <div class="zf-timeline-group-title zf-tone-${g.tone}">
        <mdui-icon name="${g.icon}"></mdui-icon>
        ${g.title}
      </div>
      <ul class="zf-list zf-list-relaxed">
        ${g.items
          .map(
            (it) => `<li class="zf-check-item-sm">
            <mdui-icon name="check"></mdui-icon>
            <span>${it}</span>
          </li>`
          )
          .join('')}
      </ul>
    </div>
  `
}

const main = document.getElementById('main-content')!

/* 最新版本:突出卡片(默认展开) */
const latest = getLatestRelease()
/* 老版本:折叠列表 */
const older = getOlderReleases()

main.innerHTML = `
  <div class="page-header">
    <div class="container">
      <mdui-icon class="page-header-icon animate-scale" name="history"></mdui-icon>
      <h1 class="page-title animate-in delay-1">更新日志</h1>
      <p class="page-desc animate-in delay-2">ZhuFiler 持续更新中。每个版本都带来新功能和改进。</p>
    </div>
  </div>

  <div class="section">
    <div class="container-narrow">

      <!-- 最新版本:突出卡片 -->
      <div class="zf-latest-card animate-scale">
        <div class="zf-latest-card-bar"></div>
        <div class="zf-latest-card-content">
          <div class="zf-latest-card-header">
            <div>
              <div class="zf-latest-card-eyebrow">
                <mdui-icon name="auto_awesome"></mdui-icon>
                最新发布
              </div>
              <div class="zf-latest-card-version">${latest.date}</div>
            </div>
            <mdui-button href="download.html" icon="download" variant="filled">下载 v1.9</mdui-button>
          </div>
          <div class="zf-latest-card-summary">${latest.summary}</div>
          ${latest.groups.map(renderGroup).join('')}
        </div>
      </div>

      <!-- 历史版本:默认折叠 -->
      <h2 class="zf-block-title animate-in">
        <mdui-icon name="archive"></mdui-icon>
        历史版本
      </h2>

      <mdui-list class="zf-history-list animate-in delay-1">
        <mdui-collapse>
          ${older
            .map(
              (v) => `
            <mdui-collapse-item value="${v.value}">
              <mdui-list-item slot="header" icon="inventory_2">
                <div class="zf-history-item">
                  <div>
                    <div class="zf-history-version">${v.date}</div>
                    <div class="zf-history-summary">${v.summary}</div>
                  </div>
                </div>
                <mdui-icon slot="end-icon" name="expand_more"></mdui-icon>
              </mdui-list-item>
              <div class="zf-history-content">
                ${v.groups.map(renderGroup).join('')}
              </div>
            </mdui-collapse-item>
          `
            )
            .join('')}
        </mdui-collapse>
      </mdui-list>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="route"></mdui-icon>
        未来计划
      </h2>

      <div class="grid grid-2 animate-in delay-1 zf-block-space">
        ${PLANS.map(
          (p) => `
          <mdui-card class="zf-card" variant="outlined">
            <div class="zf-subtitle-sm zf-tone-${p.tone}">${p.version}</div>
            <ul class="zf-list zf-list-relaxed">
              ${p.items
                .map(
                  (it) => `<li class="zf-check-item-sm">
                  <mdui-icon name="schedule" class="zf-tone-outline"></mdui-icon>
                  <span>${it}</span>
                </li>`
                )
                .join('')}
            </ul>
          </mdui-card>
        `
        ).join('')}
      </div>

      <h2 class="zf-block-title animate-in">
        <mdui-icon name="compare"></mdui-icon>
        版本对比
      </h2>

      <div class="zf-table-wrap animate-in delay-1 zf-block-space">
        <table>
          <thead>
            <tr><th>功能</th><th>v1.6</th><th>v1.7</th><th>v1.8</th><th>v1.9</th></tr>
          </thead>
          <tbody>
            ${COMPARE.map(
              (c) => `<tr>
                <td>${c.feat}</td>
                ${c.cells
                  .map(
                    (ok) => `<td>${ok ? '<mdui-icon name="check" class="zf-tone-primary"></mdui-icon>' : '<mdui-icon name="close" class="zf-tone-error"></mdui-icon>'}</td>`
                  )
                  .join('')}
              </tr>`
            ).join('')}
          </tbody>
        </table>
      </div>

      <div class="cta-section animate-scale">
        <h2>查看源码</h2>
        <p>每个版本的详细变更可以在 GitHub 上查看。</p>
        <div class="cta-actions">
          <mdui-button href="https://github.com/Artzhu86/ZhuFiler/commits/main" target="_blank" rel="noopener" icon="code" variant="filled">查看 Commits</mdui-button>
        </div>
      </div>

    </div>
  </div>
`
