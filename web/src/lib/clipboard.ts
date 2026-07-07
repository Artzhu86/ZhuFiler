/**
 * 工具函数:复制文本到剪贴板 + Snackbar 反馈
 */
import { snackbar } from 'mdui/functions/snackbar.js'

/**
 * 复制文本到剪贴板,失败时回退到 textarea 方案
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      return true
    }
    // 回退方案
    const ta = document.createElement('textarea')
    ta.value = text
    ta.style.position = 'fixed'
    ta.style.opacity = '0'
    document.body.appendChild(ta)
    ta.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(ta)
    return ok
  } catch {
    return false
  }
}

/**
 * 复制并通过 Snackbar 反馈结果
 */
export async function copyWithFeedback(text: string, successMsg = '已复制', failMsg = '复制失败') {
  const ok = await copyToClipboard(text)
  snackbar({
    message: ok ? successMsg : failMsg,
    placement: 'bottom-end',
    autoCloseDelay: 2000,
  })
  return ok
}

/**
 * 从 DOM 元素提取纯文本(去除所有 HTML 标签)
 */
export function getElementText(id: string): string {
  const el = document.getElementById(id)
  return el ? (el.textContent || '').replace(/\n{3,}/g, '\n\n').trim() : ''
}
