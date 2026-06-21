import test from 'node:test'
import assert from 'node:assert/strict'

import { unwrapApiPayload, unwrapApiField } from '../src/api/response.js'

test('unwrapApiPayload returns top-level object payload', () => {
  const payload = {
    token: 'jwt-token',
    username: 'admin',
  }

  assert.deepEqual(unwrapApiPayload(payload, '请求失败'), payload)
})

test('unwrapApiPayload returns nested data payload', () => {
  const nested = {
    token: 'jwt-token',
    username: 'admin',
  }

  assert.deepEqual(
    unwrapApiPayload(
      {
        success: true,
        data: nested,
      },
      '请求失败'
    ),
    nested
  )
})

test('unwrapApiPayload throws logical backend error', () => {
  assert.throws(
    () =>
      unwrapApiPayload(
        {
          success: false,
          error: '账号或密码错误',
        },
        '请求失败'
      ),
    /账号或密码错误/
  )
})

test('unwrapApiField returns top-level named field', () => {
  const sessions = [{ session_id: 'demo-1' }]

  assert.deepEqual(
    unwrapApiField(
      {
        sessions,
      },
      'sessions',
      [],
      '获取会话失败'
    ),
    sessions
  )
})

test('unwrapApiField returns nested named field from data wrapper', () => {
  const messages = [{ message_id: 'msg-1' }]

  assert.deepEqual(
    unwrapApiField(
      {
        success: true,
        data: {
          messages,
        },
      },
      'messages',
      [],
      '获取消息失败'
    ),
    messages
  )
})

test('unwrapApiField returns top-level array payload when endpoint responds without field wrapper', () => {
  const pendingReviews = [{ id: 1 }, { id: 2 }]

  assert.deepEqual(
    unwrapApiField(pendingReviews, 'reviews', [], '获取列表失败'),
    pendingReviews
  )
})
