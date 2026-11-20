/**
 * 资讯图片映射工具
 * 根据资讯标题返回本地图片路径
 */

// 资讯标题关键字到本地图片的映射
const newsImageMap = {
  'C919': '/images/news/C919.jpg'
}

// 默认占位图片
const defaultImage = 'https://via.placeholder.com/300x180?text=No+Image'

/**
 * 根据资讯标题获取本地图片
 * @param {string} title 资讯标题
 * @param {string} fallback 后备图片URL（数据库中的图片）
 * @returns {string} 图片路径
 */
export function getNewsImage(title, fallback) {
  if (!title) {
    return fallback || defaultImage
  }

  // 模糊匹配：检查标题是否包含映射中的关键字
  for (const key in newsImageMap) {
    if (title.includes(key)) {
      return newsImageMap[key]
    }
  }

  // 没有匹配到本地图片，使用数据库中的图片或默认图片
  return fallback || defaultImage
}

export default {
  getNewsImage
}
