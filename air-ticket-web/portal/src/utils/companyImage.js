// Database paths are authoritative. No fuzzy brand matching or external fallback.
export const defaultImage = '/images/airlines/placeholder.svg'
export function getCompanyImage(name, path) {
  return typeof path === 'string' && path.startsWith('/images/') && !path.includes('..') ? path : defaultImage
}
export function companyImageError(event) {
  if (event.target.getAttribute('src') !== defaultImage) event.target.src = defaultImage
}
export default { getCompanyImage, companyImageError }
