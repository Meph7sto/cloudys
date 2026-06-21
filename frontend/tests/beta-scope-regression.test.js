import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const srcRoot = resolve(process.cwd(), 'frontend', 'src')

function readSource(relativePath) {
  return readFileSync(resolve(srcRoot, relativePath), 'utf8')
}

test('AccountView routes the My Files entry to beta-my-files', () => {
  const source = readSource('views/beta/AccountView.vue')

  assert.match(source, /router\.push\(\{\s*name:\s*'beta-my-files'\s*\}\)/)
  assert.doesNotMatch(source, /router\.push\(\{\s*name:\s*'beta-account'\s*\}\)/)
})

test('RequirementsCollectionView keeps the advanced entry navigation', () => {
  const source = readSource('views/beta/RequirementsCollectionView.vue')

  assert.match(
    source,
    /function openAdvanced\(\)\s*\{\s*router\.push\(\{\s*name:\s*'beta-collection-advanced'\s*\}\)\s*\}/s
  )
})

test('RequirementsView keeps the defects deep-link flow', () => {
  const source = readSource('views/beta/RequirementsView.vue')

  assert.match(source, /function gotoDefectsWithRequirement\(\)\s*\{/)
  assert.doesNotMatch(source, /function gotoDefectsWithRequirement\(\)\s*\{\s*void route\s*\}/s)
  assert.match(source, /name:\s*'beta-defects'/)
  assert.match(source, /return_route:/)
})

test('Sidebar excludes out-of-scope beta review and multimodal nav items', () => {
  const source = readSource('components/beta/Sidebar.vue')

  assert.doesNotMatch(source, /\{ key: "multimodal-ingestion", label: "多模态导入"/)
  assert.doesNotMatch(source, /\{ key: "reviews", label: "评审管理"/)
})

test('Dashboard shortcuts no longer navigate to excluded review or invalid tracking pages', () => {
  const heroSource = readSource('components/beta/HeroSection.vue')
  const matrixSource = readSource('components/beta/AvailabilityMatrix.vue')

  assert.doesNotMatch(heroSource, /\$emit\('navigate', 'reviews'\)/)
  assert.doesNotMatch(heroSource, /\$emit\('navigate', 'tracking'\)/)
  assert.match(heroSource, /\$emit\('navigate', 'traceability'\)/)
  assert.doesNotMatch(matrixSource, /\$emit\('navigate', 'reviews'\)/)
})
