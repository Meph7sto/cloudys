import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'

import GuideView from '../views/GuideView.vue'
import LoginView from '../views/beta/LoginView.vue'
import RegisterView from '../views/beta/RegisterView.vue'
import DashboardView from '../views/beta/DashboardView.vue'
import ProjectManageView from '../views/beta/ProjectManageView.vue'
import ProductManageView from '../views/beta/ProductManageView.vue'
import RequirementsCollectionView from '../views/beta/RequirementsCollectionView.vue'
import RequirementsAnalysisView from '../views/beta/RequirementsAnalysisView.vue'
import RequirementsView from '../views/beta/RequirementsView.vue'
import RequirementsSessionView from '../views/beta/RequirementsSessionView.vue'
import AccountView from '../views/beta/AccountView.vue'
import UserManageView from '../views/beta/UserManageView.vue'
import DefectsView from '../views/beta/DefectsView.vue'
import TraceabilityView from '../views/beta/TraceabilityView.vue'
import ReportView from '../views/beta/ReportView.vue'
import RequirementGraphView from '../views/beta/RequirementGraphView.vue'
import RequirementsAcquisitionView from '../views/beta/RequirementsAcquisitionView.vue'
import CollectionAdvancedView from '../views/beta/CollectionAdvancedView.vue'
import MyFilesView from '../views/beta/MyFilesView.vue'
import RequirementsManageWorkbenchView from '../views/beta/RequirementsManageWorkbenchView.vue'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            redirect: '/guide',
        },
        {
            path: '/guide',
            name: 'guide',
            component: GuideView,
            meta: { requiresAuth: false },
        },
        {
            path: '/beta',
            name: 'beta',
            redirect: '/beta/login',
            children: [
                {
                    path: 'login',
                    name: 'beta-login',
                    component: LoginView,
                    meta: { requiresAuth: false },
                },
                {
                    path: 'register',
                    name: 'beta-register',
                    component: RegisterView,
                    meta: { requiresAuth: false },
                },
                {
                    path: 'dashboard',
                    name: 'beta-dashboard',
                    component: DashboardView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'project',
                    name: 'beta-project',
                    component: ProjectManageView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'products',
                    name: 'beta-products',
                    component: ProductManageView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-collection',
                    name: 'beta-requirements-collection',
                    component: RequirementsCollectionView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-analysis',
                    name: 'beta-requirements-analysis',
                    component: RequirementsAnalysisView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements',
                    name: 'beta-requirements',
                    component: RequirementsView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-session',
                    name: 'beta-requirements-session',
                    component: RequirementsSessionView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'account',
                    name: 'beta-account',
                    component: AccountView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'user-manage',
                    name: 'beta-user-manage',
                    component: UserManageView,
                    meta: {
                        requiresAuth: true,
                        requiredSystemIdentities: ['SYS_ADMIN'],
                    },
                },
                {
                    path: 'defects',
                    name: 'beta-defects',
                    component: DefectsView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'traceability',
                    name: 'beta-traceability',
                    component: TraceabilityView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'multimodal-ingestion',
                    name: 'beta-multimodal-ingestion',
                    redirect: { name: 'beta-requirements-collection' },
                    meta: { requiresAuth: true },
                },
                {
                    path: 'reports',
                    name: 'beta-reports',
                    component: ReportView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirement-graph',
                    name: 'beta-requirement-graph',
                    component: RequirementGraphView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-acquisition',
                    name: 'beta-requirements-acquisition',
                    component: RequirementsAcquisitionView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'reviews',
                    name: 'beta-reviews',
                    redirect: { name: 'beta-defects' },
                    meta: { requiresAuth: true },
                },
                {
                    path: 'collection-advanced',
                    name: 'beta-collection-advanced',
                    component: CollectionAdvancedView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'my-files',
                    name: 'beta-my-files',
                    component: MyFilesView,
                    meta: { requiresAuth: true },
                },
                {
                    path: 'requirements-manage',
                    name: 'beta-requirements-manage',
                    component: RequirementsManageWorkbenchView,
                    meta: { requiresAuth: true },
                },
            ],
        },
        {
            path: '/:pathMatch(.*)*',
            redirect: '/guide',
        },
    ],
})

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore()
    authStore.init()

    const requiresAuth = to.meta?.requiresAuth !== false
    if (requiresAuth) {
        const isAuthorized = await authStore.ensureValidSession()
        if (!isAuthorized) {
            next({
                name: 'beta-login',
                query: { redirect: to.fullPath },
            })
            return
        }
    }

    if (Array.isArray(to.meta?.requiredSystemIdentities) && to.meta.requiredSystemIdentities.length > 0) {
        const currentIdentity = authStore.systemIdentity
        if (!to.meta.requiredSystemIdentities.includes(currentIdentity)) {
            next({ name: 'beta-dashboard' })
            return
        }
    }

    next()
})

export default router
