/**
 * GitHub API 共享模块
 * 抽取 home.ts 和 about.ts 中重复的仓库数据获取与数字动画
 */

const REPO = 'Artzhu86/ZhuFiler'
const API_BASE = 'https://api.github.com'

export interface RepoStats {
  stars: number
  forks: number
  watchers: number
}

export interface Contributor {
  login: string
  avatar_url: string
  html_url: string
}

export interface RepoStatsWithContribs extends RepoStats {
  contributors: Contributor[]
}

/**
 * 数字滚动动画(ease-out cubic)
 */
export function animateNumber(id: string, target: number, duration = 1800) {
  const node: HTMLElement | null = document.getElementById(id)
  if (!node) return
  const start = performance.now()
  function tick(now: number) {
    const p = Math.min((now - start) / duration, 1)
    const ease = 1 - Math.pow(1 - p, 3)
    if (node) node.textContent = Math.floor(target * ease).toLocaleString()
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

/**
 * 获取仓库基础数据(stars/forks/watchers)
 */
export async function fetchRepoStats(): Promise<RepoStats | null> {
  try {
    const res = await fetch(`${API_BASE}/repos/${REPO}`)
    if (!res.ok) return null
    const data = await res.json()
    return {
      stars: data.stargazers_count,
      forks: data.forks_count,
      watchers: data.watchers_count,
    }
  } catch {
    return null
  }
}

/**
 * 获取仓库贡献者列表
 */
export async function fetchContributors(): Promise<Contributor[]> {
  try {
    const res = await fetch(`${API_BASE}/repos/${REPO}/contributors`)
    if (!res.ok) return []
    return await res.json()
  } catch {
    return []
  }
}

/**
 * 失败时显示 N/A
 */
export function setStatsFallback(ids: string[]) {
  ids.forEach((id) => {
    const el = document.getElementById(id)
    if (el) el.textContent = 'N/A'
  })
}
