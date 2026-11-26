import type { IconName } from '../components/ui/icons'

export type SiteNavGroupId = 'conference' | 'service' | 'account'

export interface SiteNavItem {
  to: string
  label: string
  icon: IconName
  /** 可选的一句站内现成说明（仅展示在桌面下拉面板第二行） */
  description?: string
  /** 匹配子路由高亮的前缀（默认按 to 匹配） */
  matchPrefixes?: string[]
}

export interface SiteNavFeaturedCard {
  image: string
  tag: string
  title: string
  description: string
  to: string
}

export interface SiteNavGroup {
  id: SiteNavGroupId
  label: string
  items: SiteNavItem[]
  featured?: SiteNavFeaturedCard
}

/**
 * 全站导航、页尾与移动端抽屉的单一数据源。
 * 所有路由路径均与 `src/router.ts` 保持一致。
 */
export const siteNavGroups: SiteNavGroup[] = [
  {
    id: 'conference',
    label: '大会',
    items: [
      {
        to: '/activities',
        label: '活动日程',
        icon: 'calendar',
        description: '议程查看与在线报名',
        matchPrefixes: ['/activit']
      },
      {
        to: '/articles',
        label: '新闻资讯',
        icon: 'book-open',
        matchPrefixes: ['/article']
      },
      {
        to: '/submissions',
        label: '用户投稿',
        icon: 'edit',
        description: '分享参会见闻与观点',
        matchPrefixes: ['/submissions', '/submit']
      },
      {
        to: '/exhibitors',
        label: '参展单位',
        icon: 'building'
      },
      {
        to: '/achievements',
        label: '成果展示',
        icon: 'medal'
      }
    ],
    featured: {
      image: '/hero/materials/robot-lab-arms.jpg',
      tag: '大会',
      title: '西湖论剑 · 数字安全大会',
      description: '汇集大会议程、活动报名、资料下载与现场回顾。',
      to: '/activities'
    }
  },
  {
    id: 'service',
    label: '服务',
    items: [
      {
        to: '/payment',
        label: '购票',
        icon: 'ticket'
      },
      {
        to: '/materials',
        label: '资料下载',
        icon: 'download',
        description: '演讲资料与大会手册'
      },
      {
        to: '/media',
        label: '媒体中心',
        icon: 'video',
        description: '现场图片与视频回放'
      },
      {
        to: '/statistics',
        label: '数据看板',
        icon: 'bar-chart'
      }
    ],
    featured: {
      image: '/hero/materials/touch-screen-wall.jpg',
      tag: '服务',
      title: '参会服务',
      description: '为会议活动提供一站式数字安全支撑与智能运营能力。',
      to: '/payment'
    }
  },
  {
    id: 'account',
    label: '账户',
    items: [
      { to: '/profile', label: '个人信息', icon: 'user' },
      { to: '/orders', label: '我的订单', icon: 'shopping-cart' },
      { to: '/points', label: '积分详情', icon: 'star', matchPrefixes: ['/points', '/checkin'] }
    ]
  }
]

/** 桌面顶栏中间带下拉面板的分组（大会 / 服务） */
export const primaryNavGroups = siteNavGroups.filter(
  (group): group is SiteNavGroup & { featured: SiteNavFeaturedCard } => Boolean(group.featured)
)

/** 账户分组（头像菜单、抽屉与页尾复用） */
export const accountNavGroup = siteNavGroups.find((group) => group.id === 'account')!

export function isNavItemActive(item: SiteNavItem, path: string): boolean {
  const prefixes = item.matchPrefixes?.length ? item.matchPrefixes : [item.to]
  return prefixes.some((prefix) => path === prefix || path.startsWith(prefix))
}

export function isNavGroupActive(group: SiteNavGroup, path: string): boolean {
  return group.items.some((item) => isNavItemActive(item, path))
}
