// IconMCP MIT SVG 图标注册表（由 iconmcp.com 获取）
import arrowLeft from './arrow-left.svg?raw'
import arrowRight from './arrow-right.svg?raw'
import chevronDown from './chevron-down.svg?raw'
import menu from './menu.svg?raw'
import x from './x.svg?raw'
import check from './check.svg?raw'
import checkCircle from './check-circle.svg?raw'
import alertTriangle from './alert-triangle.svg?raw'
import bell from './bell.svg?raw'
import calendar from './calendar.svg?raw'
import camera from './camera.svg?raw'
import messageCircle from './message-circle.svg?raw'
import globe from './globe.svg?raw'
import cpu from './cpu.svg?raw'
import creditCard from './credit-card.svg?raw'
import trash from './trash.svg?raw'
import fileText from './file-text.svg?raw'
import download from './download.svg?raw'
import edit from './edit.svg?raw'
import grid from './grid.svg?raw'
import home from './home.svg?raw'
import zap from './zap.svg?raw'
import link from './link.svg?raw'
import list from './list.svg?raw'
import mapPin from './map-pin.svg?raw'
import lock from './lock.svg?raw'
import wallet from './wallet.svg?raw'
import monitor from './monitor.svg?raw'
import building from './building.svg?raw'
import image from './image.svg?raw'
import plus from './plus.svg?raw'
import send from './send.svg?raw'
import bookOpen from './book-open.svg?raw'
import refreshCw from './refresh-cw.svg?raw'
import share from './share.svg?raw'
import shoppingCart from './shopping-cart.svg?raw'
import star from './star.svg?raw'
import logOut from './log-out.svg?raw'
import ticket from './ticket.svg?raw'
import trendingUp from './trending-up.svg?raw'
import upload from './upload.svg?raw'
import user from './user.svg?raw'
import userCircle from './user-circle.svg?raw'
import video from './video.svg?raw'
import playCircle from './play-circle.svg?raw'
import eye from './eye.svg?raw'
import medal from './medal.svg?raw'
import barChart from './bar-chart.svg?raw'

export const iconMap = {
  'arrow-left': arrowLeft,
  'arrow-right': arrowRight,
  'chevron-down': chevronDown,
  'menu': menu,
  'x': x,
  'check': check,
  'check-circle': checkCircle,
  'alert-triangle': alertTriangle,
  'bell': bell,
  'calendar': calendar,
  'camera': camera,
  'message-circle': messageCircle,
  'globe': globe,
  'cpu': cpu,
  'credit-card': creditCard,
  'trash': trash,
  'file-text': fileText,
  'download': download,
  'edit': edit,
  'grid': grid,
  'home': home,
  'zap': zap,
  'link': link,
  'list': list,
  'map-pin': mapPin,
  'lock': lock,
  'wallet': wallet,
  'monitor': monitor,
  'building': building,
  'image': image,
  'plus': plus,
  'send': send,
  'book-open': bookOpen,
  'refresh-cw': refreshCw,
  'share': share,
  'shopping-cart': shoppingCart,
  'star': star,
  'log-out': logOut,
  'ticket': ticket,
  'trending-up': trendingUp,
  'upload': upload,
  'user': user,
  'user-circle': userCircle,
  'video': video,
  'play-circle': playCircle,
  'eye': eye,
  'medal': medal,
  'bar-chart': barChart,
} as const

export type IconName = keyof typeof iconMap
