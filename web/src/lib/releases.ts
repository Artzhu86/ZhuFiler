/**
 * 共享版本发布数据
 * 数据来源:git tag + commit 日期(权威)
 * download 页用 getRecentReleases(2) 只显示最近 2 版
 * changelog 页用 getAllReleases() 显示完整历史
 */

/* ---------- 变更分组类型 ---------- */
export type ChangeGroup = {
  icon: string
  title: string
  tone: 'primary' | 'tertiary'
  items: string[]
}

/* ---------- 版本条目类型 ---------- */
export type Release = {
  /** 唯一标识,用于 DOM id / collapse value */
  value: string
  /** 显示用日期,如 "v1.9 · 2026年7月4日" */
  date: string
  /** 折叠态显示的摘要,如 "新增 3 项 · 改进 2 项" */
  summary: string
  /** 是否为最新发布 */
  latest?: boolean
  /** 变更分组 */
  groups: ChangeGroup[]
}

/* ---------- 完整发布历史(从新到旧) ---------- */
const RELEASES: Release[] = [
  {
    value: 'v1.9',
    date: 'v1.9 · 2026年7月4日',
    latest: true,
    summary: '修复 3 项 · 改进 4 项',
    groups: [
      {
        icon: 'check_circle',
        title: '改进',
        tone: 'primary',
        items: ['修复了若干bug', '优化了性能和稳定性', '更新了依赖库', '改进了错误处理'],
      },
      {
        icon: 'bug_report',
        title: '修复',
        tone: 'primary',
        items: [
          '修复了某些设备上的崩溃问题',
          '修复了文件搜索的内存泄漏',
          '修复了书签显示异常',
        ],
      },
    ],
  },
  {
    value: 'v1.8',
    date: 'v1.8 · 2026年7月2日',
    summary: '新增 3 项 · 改进 2 项',
    groups: [
      {
        icon: 'star',
        title: '新功能',
        tone: 'tertiary',
        items: ['新增文件搜索功能', '支持书签管理', '改进了多选操作'],
      },
      {
        icon: 'check_circle',
        title: '改进',
        tone: 'primary',
        items: ['优化了文件加载速度', '改进了手势操作体验'],
      },
    ],
  },
  {
    value: 'v1.7',
    date: 'v1.7 · 2026年6月29日',
    summary: '新增 3 项 · 改进 2 项',
    groups: [
      {
        icon: 'star',
        title: '新功能',
        tone: 'tertiary',
        items: ['添加代码高亮查看器', '支持13种编程语言', '新增图片缩放功能'],
      },
      {
        icon: 'check_circle',
        title: '改进',
        tone: 'primary',
        items: ['改进了文件预览体验', '优化了内存使用'],
      },
    ],
  },
  {
    value: 'v1.6',
    date: 'v1.6 · 2026年6月28日',
    summary: '首次发布',
    groups: [
      {
        icon: 'star',
        title: '新功能',
        tone: 'tertiary',
        items: ['初始版本发布', '基础文件浏览功能', 'Material 3 界面设计'],
      },
    ],
  },
]

/* ---------- 未来计划(单独维护,与发布版本无关) ---------- */
export type Plan = { version: string; tone: 'primary' | 'tertiary'; items: string[] }

export const PLANS: Plan[] = [
  {
    version: 'v1.10 计划中',
    tone: 'primary',
    items: ['更多文件格式支持', '云端同步功能', '性能优化'],
  },
  {
    version: 'v2.0 规划中',
    tone: 'tertiary',
    items: ['Material You 动态颜色', '更多手势操作', '插件系统'],
  },
]

/* ---------- 公共 API ---------- */

/** 获取全部发布历史(从新到旧) */
export function getAllReleases(): Release[] {
  return RELEASES
}

/** 获取最近 N 个发布 */
export function getRecentReleases(n: number): Release[] {
  return RELEASES.slice(0, n)
}

/** 获取最新发布 */
export function getLatestRelease(): Release {
  return RELEASES[0]
}

/** 获取除最新版外的历史(用于 changelog 折叠区) */
export function getOlderReleases(): Release[] {
  return RELEASES.slice(1)
}
