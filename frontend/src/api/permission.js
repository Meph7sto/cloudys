import { api } from './request'
import { unwrapApiPayload } from './response'

// ========================
// 权限管理 API
// ========================
export const permissionApi = {
  /**
   * 获取用户在项目中的权限上下文
   */
  async getProjectContext(projectId) {
    const resp = await api.get(`/permission/projects/${encodeURIComponent(projectId)}/context`)
    return unwrapApiPayload(resp.data, '获取权限上下文失败')
  },

  // ========================
  // 项目成员管理
  // ========================

  /**
   * 添加项目成员
   * @param {string} projectId - 项目 ID
   * @param {string} userId - 用户 ID
   * @param {string[]} memberRoles - 角色列表（可选）
   */
  async addProjectMember(projectId, userId, memberRoles = []) {
    const resp = await api.post(`/permission/projects/${encodeURIComponent(projectId)}/members`, {
      user_id: userId,
      member_roles: memberRoles,
    })
    return unwrapApiPayload(resp.data, '添加项目成员失败')
  },

  /**
   * 列出项目成员
   * @param {string} projectId - 项目 ID
   * @param {boolean} includeRoles - 是否包含角色信息
   */
  async listProjectMembers(projectId, includeRoles = false) {
    const resp = await api.get(`/permission/projects/${encodeURIComponent(projectId)}/members`, {
      params: { include_roles: includeRoles ? 'true' : 'false' },
    })
    return unwrapApiPayload(resp.data, '获取项目成员列表失败')
  },

  /**
   * 移除项目成员
   * @param {string} projectId - 项目 ID
   * @param {string} userId - 用户 ID
   */
  async removeProjectMember(projectId, userId) {
    const resp = await api.delete(`/permission/projects/${encodeURIComponent(projectId)}/members/${encodeURIComponent(userId)}`)
    return unwrapApiPayload(resp.data, '移除项目成员失败')
  },

  /**
   * 设置用户角色（全量替换）
   * @param {string} projectId - 项目 ID
   * @param {string} userId - 用户 ID
   * @param {string[]} roles - 角色列表
   */
  async setUserRoles(projectId, userId, roles) {
    const resp = await api.put(`/permission/projects/${encodeURIComponent(projectId)}/members/${encodeURIComponent(userId)}/roles`, {
      roles,
    })
    return unwrapApiPayload(resp.data, '设置用户角色失败')
  },

  // ========================
  // 评审管理
  // ========================

  /**
   * 创建评审指派
   * @param {string} requirementId - 需求 ID
   * @param {string} reviewerId - 评审人 ID
   * @param {number} seq - 序号
   * @param {string} comment - 评审说明
   */
  async createReviewAssignment(requirementId, reviewerId, seq = 1, comment = null) {
    const resp = await api.post(`/permission/requirements/${encodeURIComponent(requirementId)}/reviews`, {
      reviewrer_id: reviewerId,
      seq,
      comment,
    })
    return unwrapApiPayload(resp.data, '创建评审指派失败')
  },

  /**
   * 列出需求的所有评审
   * @param {string} requirementId - 需求 ID
   * @param {string} status - 状态筛选（可选）
   */
  async listRequirementReviews(requirementId, status = null) {
    const params = {}
    if (status) params.status = status

    const resp = await api.get(`/permission/requirements/${encodeURIComponent(requirementId)}/reviews`, { params })
    return unwrapApiPayload(resp.data, '获取评审列表失败')
  },

  /**
   * 通过评审
   * @param {number} assignmentId - 评审指派 ID
   * @param {string} comment - 审批意见
   */
  async approveReview(assignmentId, comment = null) {
    const resp = await api.post(`/permission/reviews/${assignmentId}/approve`, { comment })
    return unwrapApiPayload(resp.data, '通过评审失败')
  },

  /**
   * 驳回评审
   * @param {number} assignmentId - 评审指派 ID
   * @param {string} comment - 驳回原因
   */
  async rejectReview(assignmentId, comment = null) {
    const resp = await api.post(`/permission/reviews/${assignmentId}/reject`, { comment })
    return unwrapApiPayload(resp.data, '驳回评审失败')
  },

  /**
   * 撤回评审
   * @param {number} assignmentId - 评审指派 ID
   */
  async withdrawReview(assignmentId) {
    const resp = await api.post(`/permission/reviews/${assignmentId}/withdraw`)
    return unwrapApiPayload(resp.data, '撤回评审失败')
  },

  /**
   * 获取当前用户的待处理评审列表
   * @param {string} projectId - 项目 ID（可选）
   */
  async getPendingReviews(projectId = null) {
    const params = {}
    if (projectId) params.project_id = projectId

    const resp = await api.get('/permission/user/pending-reviews', { params })
    return unwrapApiPayload(resp.data, '获取待处理评审失败')
  },

  // ========================
  // 基线管理
  // ========================

  /**
   * 创建基线
   * @param {string} projectId - 项目 ID
   * @param {string} version - 版本号
   * @param {boolean} locked - 是否锁定
   */
  async createBaseline(projectId, version, locked = false) {
    const resp = await api.post(`/permission/projects/${encodeURIComponent(projectId)}/baselines`, {
      version,
      locked,
    })
    return unwrapApiPayload(resp.data, '创建基线失败')
  },

  /**
   * 列出项目的所有基线
   * @param {string} projectId - 项目 ID
   * @param {boolean} includeLocked - 是否包含已锁定的基线
   */
  async listBaselines(projectId, includeLocked = true) {
    const resp = await api.get(`/permission/projects/${encodeURIComponent(projectId)}/baselines`, {
      params: { include_locked: includeLocked ? 'true' : 'false' },
    })
    return unwrapApiPayload(resp.data, '获取基线列表失败')
  },

  /**
   * 锁定基线
   * @param {number} baselineId - 基线 ID
   */
  async lockBaseline(baselineId) {
    const resp = await api.post(`/permission/baselines/${baselineId}/lock`)
    return unwrapApiPayload(resp.data, '锁定基线失败')
  },

  /**
   * 解锁基线
   * @param {number} baselineId - 基线 ID
   */
  async unlockBaseline(baselineId) {
    const resp = await api.post(`/permission/baselines/${baselineId}/unlock`)
    return unwrapApiPayload(resp.data, '解锁基线失败')
  },

  // ========================
  // 变更请求管理
  // ========================

  /**
   * 创建变更请求
   * @param {string} requirementId - 需求 ID
   * @param {number} baselineId - 基线 ID
   * @param {string} reason - 变更原因
   * @param {string} changeSummary - 变更摘要
   */
  async createChangeRequest(requirementId, baselineId, reason, changeSummary) {
    const resp = await api.post(`/permission/requirements/${encodeURIComponent(requirementId)}/change-requests`, {
      baseline_id: baselineId,
      reason,
      change_summary: changeSummary,
    })
    return unwrapApiPayload(resp.data, '创建变更请求失败')
  },

  /**
   * 列出需求的所有变更请求
   * @param {string} requirementId - 需求 ID
   * @param {string} status - 状态筛选（可选）
   */
  async listChangeRequests(requirementId, status = null) {
    const params = {}
    if (status) params.status = status

    const resp = await api.get(`/permission/requirements/${encodeURIComponent(requirementId)}/change-requests`, { params })
    return unwrapApiPayload(resp.data, '获取变更请求列表失败')
  },

  /**
   * 批准变更请求
   * @param {number} requestId - 变更请求 ID
   * @param {string} reviewComment - 审批意见
   */
  async approveChangeRequest(requestId, reviewComment = null) {
    const resp = await api.post(`/permission/change-requests/${requestId}/approve`, {
      review_comment: reviewComment,
    })
    return unwrapApiPayload(resp.data, '批准变更请求失败')
  },

  /**
   * 驳回变更请求
   * @param {number} requestId - 变更请求 ID
   * @param {string} reviewComment - 驳回原因
   */
  async rejectChangeRequest(requestId, reviewComment = null) {
    const resp = await api.post(`/permission/change-requests/${requestId}/reject`, {
      review_comment: reviewComment,
    })
    return unwrapApiPayload(resp.data, '驳回变更请求失败')
  },

  /**
   * 获取待审批的变更请求列表
   * @param {string} projectId - 项目 ID（可选）
   */
  async getPendingChangeRequests(projectId = null) {
    const params = {}
    if (projectId) params.project_id = projectId

    const resp = await api.get('/permission/change-requests/pending', { params })
    return unwrapApiPayload(resp.data, '获取待审批变更请求失败')
  },
}
