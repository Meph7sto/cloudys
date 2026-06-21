import { api } from './request'
import { unwrapApiField, unwrapApiPayload } from './response'

// 通用问答 API
export const chatApi = {
    async streamAsk(
        prompt,
        model = 'deepseek-v4-pro',
        useThinking = true,
        useAsyncClient = false,
        maxNewTokens = 800,
        systemPrompt = ''
    ) {
        const response = await fetch('/inference/chat/stream', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                prompt,
                model,
                system_prompt: systemPrompt,
                max_new_tokens: maxNewTokens,
                use_thinking_mode: useThinking,
                use_async_client: useAsyncClient,
            }),
        })

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
        }

        return response.body
    },
}

// 移动端对话桥接 API
export const mobileChatApi = {
    async listSessions({ userId = null, limit = 200 } = {}) {
        const params = { limit }
        if (userId) params.user_id = userId
        const resp = await api.get('/mobile/sessions', { params })
        return unwrapApiField(resp.data, 'sessions', [], '获取会话列表失败')
    },

    async listMessages({ userId = null, sessionId, limit = 100, since = null } = {}) {
        const params = { session_id: sessionId, limit }
        if (userId) params.user_id = userId
        if (since) params.since = since
        const resp = await api.get('/mobile/messages', { params })
        return unwrapApiField(resp.data, 'messages', [], '获取移动端消息失败')
    },

    async sendMessage({ userId = null, sessionId, sender, content, metadata = null }) {
        const payload = {
            session_id: sessionId,
            sender,
            content,
        }
        if (userId) payload.user_id = userId
        if (metadata) payload.metadata = metadata
        const resp = await api.post('/mobile/messages', payload)
        return unwrapApiPayload(resp.data, '发送移动端消息失败')
    },

    createSSEConnection(sessionId, userId = null) {
        const params = new URLSearchParams({
            session_id: sessionId,
        })
        if (userId) params.set('user_id', userId)
        return new EventSource(`/api/v2/mobile/stream?${params.toString()}`)
    },
}
