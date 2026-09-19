export const units = ['PACKETS', 'PIECES', 'KG', 'GRAMS', 'LITRES', 'MILLILITRES', 'BAGS', 'CARTONS', 'BOXES', 'DOZENS', 'QUINTALS']
export const languages = [['en', 'English'], ['te', 'తెలుగు · Telugu'], ['hi', 'हिन्दी · Hindi'], ['mixed', 'Mixed language']]
export const quantity = value => new Intl.NumberFormat(undefined, { maximumFractionDigits: 3 }).format(value)
export const unitLabel = value => value?.toLowerCase() || ''

