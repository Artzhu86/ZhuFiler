mdui.setColorScheme('#1976D2')

const html = document.documentElement
const themeToggle = document.getElementById('theme-toggle')
const prefersDark = window.matchMedia('(prefers-color-scheme: dark)')

function applyTheme(dark) {
  if (dark) html.classList.add('mdui-theme-dark')
  else html.classList.remove('mdui-theme-dark')
  themeToggle?.setAttribute('icon', dark ? 'light_mode' : 'dark_mode')
}

applyTheme(prefersDark.matches)

prefersDark.addEventListener('change', (e) => {
  applyTheme(e.matches)
})

themeToggle?.addEventListener('click', () => {
  applyTheme(!html.classList.contains('mdui-theme-dark'))
})

const topBar = document.getElementById('top-bar')
const layoutMain = document.querySelector('.zf-layout-main')
function updateTopBar() {
  topBar.classList.toggle('scrolled', layoutMain.scrollTop > 8)
}
layoutMain.addEventListener('scroll', updateTopBar, { passive: true })
updateTopBar()

const navDrawer = document.getElementById('nav-drawer')
document.getElementById('drawer-toggle').addEventListener('click', () => {
  navDrawer.open = !navDrawer.open
})
navDrawer.querySelectorAll('mdui-list-item').forEach(item => {
  item.addEventListener('click', () => {
    navDrawer.open = false
    if (item.dataset.nav) {
      navDrawer.querySelectorAll('mdui-list-item[data-nav]').forEach(i => i.removeAttribute('active'))
      item.setAttribute('active', '')
      if (item.dataset.nav === 'top') {
        layoutMain.scrollTo({ top: 0, behavior: 'smooth' })
      } else {
        const el = document.getElementById(item.dataset.nav)
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    }
  })
})

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

const navItems = navDrawer.querySelectorAll('mdui-list-item[data-nav]')
const navObserver = new IntersectionObserver((entries) => {
  entries.forEach(entry => {
    if (entry.isIntersecting) {
      const id = entry.target.id || 'top'
      navItems.forEach(item => {
        if (item.dataset.nav === id) item.setAttribute('active', '')
        else item.removeAttribute('active')
      })
    }
  })
}, { rootMargin: '-50% 0px -50% 0px' })
document.querySelectorAll('.hero, #previews, #features').forEach(el => navObserver.observe(el))

document.getElementById('download-btn').addEventListener('click', async () => {
  try {
    const res = await fetch('https://api.github.com/repos/Artzhu86/ZhuFiler/releases/latest')
    const data = await res.json()
    const apk = data.assets.find(a => a.name.endsWith('.apk'))
    if (apk) {
      const url = apk.browser_download_url
      const proxies = ['https://gh.zwy.one/', 'https://ghproxy.net/']
      for (const proxy of proxies) {
        try {
          const ctrl = new AbortController()
          setTimeout(() => ctrl.abort(), 3000)
          await fetch(proxy + url, { method: 'HEAD', signal: ctrl.signal })
          window.location.href = proxy + url
          return
        } catch {
          continue
        }
      }
      window.location.href = url
    } else {
      window.open('https://github.com/Artzhu86/ZhuFiler/releases/latest', '_blank')
    }
  } catch {
    window.open('https://github.com/Artzhu86/ZhuFiler/releases/latest', '_blank')
  }
})
