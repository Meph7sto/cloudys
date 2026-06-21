import test from 'node:test'
import assert from 'node:assert/strict'

import { unwrapAuthPayload } from '../src/api/auth-response.js'

test('unwrapAuthPayload returns top-level auth payload when backend responds without data wrapper', () => {
  const payload = {
    token: 'jwt-token',
    username: 'admin',
    role: 'super_admin',
  }

  assert.deepEqual(unwrapAuthPayload(payload, '登录失败'), payload)
})

test('unwrapAuthPayload returns nested data payload when backend responds with standard wrapper', () => {
  const nested = {
    token: 'jwt-token',
    username: 'admin',
    role: 'super_admin',
  }

  assert.deepEqual(
    unwrapAuthPayload(
      {
        success: true,
        data: nested,
      },
      '登录失败'
    ),
    nested
  )
})

test('unwrapAuthPayload throws backend auth error when request is rejected logically', () => {
  assert.throws(
    () =>
      unwrapAuthPayload(
        {
          success: false,
          error: '账号或密码错误',
        },
        '登录失败'
      ),
    /账号或密码错误/
  )
})

test('unwrapAuthPayload returns top-level array payload when backend responds without data wrapper', () => {
  const payload = [
    { id: 1, title: 'review-a' },
    { id: 2, title: 'review-b' },
  ]

  assert.deepEqual(unwrapAuthPayload(payload, '获取列表失败'), payload)
})

test('unwrapAuthPayload returns nested array payload when backend responds with standard wrapper', () => {
  const nested = [
    { id: 1, title: 'review-a' },
    { id: 2, title: 'review-b' },
  ]

  assert.deepEqual(
    unwrapAuthPayload(
      {
        success: true,
        data: nested,
      },
      '获取列表失败'
    ),
    nested
  )
})
