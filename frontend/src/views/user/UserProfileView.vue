<template>
  <div v-loading="loading" class="profile-page">
    <template v-if="profile">
      <!-- 资料头 -->
      <div class="profile-head">
        <el-avatar :size="76" :src="profile.user.avatar || undefined">{{ profile.user.nickname?.charAt(0) }}</el-avatar>
        <div class="who">
          <div class="name-row">
            <h1>{{ profile.user.nickname }}</h1>
            <el-tag v-if="profile.user.isExpert" type="warning" effect="plain" size="small">探店达人</el-tag>
          </div>
          <p class="intro">{{ profile.user.intro || '这个人很懒，还没有写简介。' }}</p>
          <p class="meta">菜谱 {{ profile.recipeCount }} 道 · 探店 {{ profile.storeVisitCount }} 篇</p>
        </div>
        <div v-if="profile.user.isExpert && isOtherUser" class="head-actions">
          <el-button type="primary" round @click="openReserve">📅 预约探店</el-button>
          <el-button round @click="goChat">💬 私聊</el-button>
        </div>
      </div>

      <!-- 作品切换 -->
      <div class="tabs">
        <span class="tab" :class="{ active: tab === 'recipe' }" @click="switchTab('recipe')">菜谱作品</span>
        <span class="tab" :class="{ active: tab === 'store' }" @click="switchTab('store')">探店足迹</span>
      </div>

      <div v-loading="tabLoading" class="works">
        <!-- 菜谱 -->
        <div v-if="tab === 'recipe'">
          <div class="grid">
            <router-link v-for="r in rows" :key="r.id" :to="`/recipe/${r.id}`" class="rc card-link">
              <div class="thumb"><RecipeCover :src="r.cover" :title="r.title" :seed="r.id" /></div>
              <div class="body">
                <h3>{{ r.title }}</h3>
                <div class="meta2">{{ r.categoryName }} · {{ r.views }} 次浏览</div>
              </div>
            </router-link>
          </div>
          <el-empty v-if="!tabLoading && !rows.length" description="还没有通过审核的菜谱" />
        </div>

        <!-- 探店 -->
        <div v-else>
          <div class="grid">
            <div v-for="sv in rows" :key="sv.id" class="rc card-link sv-card" @click="openSv(sv)">
              <div class="thumb"><RecipeCover :src="sv.cover" :title="sv.title" :seed="sv.id" /></div>
              <div class="body">
                <h3>{{ sv.title }}</h3>
                <div class="meta2">{{ sv.address || '未标注地址' }} · {{ sv.views }} 次浏览</div>
              </div>
            </div>
          </div>
          <el-empty v-if="!tabLoading && !rows.length" description="还没有通过审核的探店足迹" />
        </div>

        <el-pagination v-if="total > pageSize" class="pager" background layout="prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page" @current-change="loadTab" />
      </div>

      <!-- 探店详情弹窗 -->
      <el-dialog v-model="svVisible" :title="currentSv?.title" width="640px">
        <div v-if="currentSv" class="sv-body">
          <div class="sv-cover"><RecipeCover :src="currentSv.cover" :title="currentSv.title" :seed="currentSv.id" /></div>
          <div class="sv-meta">
            <span>📍 {{ currentSv.address || '未标注地址' }}</span>
            <span>👁 {{ currentSv.views }}</span>
            <span>发布于 {{ currentSv.createTime }}</span>
          </div>
          <MarkdownContent :content="currentSv.content" />
        </div>
      </el-dialog>

      <!-- 预约表单 -->
      <el-dialog v-model="reserveVisible" title="预约探店" width="520px">
        <el-form :model="reserveForm" :rules="reserveRules" ref="reserveFormRef" label-width="90px">
          <el-form-item label="达人">
            <span>{{ profile.user.nickname }}</span>
          </el-form-item>
          <el-form-item label="店铺名称" prop="storeName">
            <el-input v-model="reserveForm.storeName" maxlength="100" />
          </el-form-item>
          <el-form-item label="店铺介绍" prop="storeIntro">
            <el-input v-model="reserveForm.storeIntro" type="textarea" :rows="2" maxlength="255" />
          </el-form-item>
          <el-form-item label="店铺地址" prop="storeAddress">
            <el-input v-model="reserveForm.storeAddress" maxlength="255" />
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="reserveForm.phone" maxlength="20" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="reserveForm.remark" type="textarea" :rows="2" maxlength="500"
                      placeholder="希望什么时候探店、有没有忌口…" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="reserveVisible = false">取消</el-button>
          <el-button type="primary" :loading="reserving" @click="submitReserve">提交预约</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authorProfile, authorRecipes } from '@/api/interaction'
import { authorStoreVisits, storeVisitDetail, createReservation } from '@/api/storeVisit'
import { useUserStore } from '@/stores/user'
import RecipeCover from '@/components/RecipeCover.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const profile = ref(null)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const loading = ref(true)
const tabLoading = ref(false)
const tab = ref('recipe')

const svVisible = ref(false)
const currentSv = ref(null)
const reserveVisible = ref(false)
const reserving = ref(false)
const reserveFormRef = ref()

const reserveForm = reactive({ storeName: '', storeIntro: '', storeAddress: '', phone: '', remark: '' })
const reserveRules = {
  storeName: [{ required: true, message: '请填写店铺名称', trigger: 'blur' }]
}

const isOtherUser = computed(() => !store.isLogin || store.user?.id !== Number(route.params.id))

async function loadTab(p) {
  page.value = p
  tabLoading.value = true
  try {
    const params = { page: p, size: pageSize }
    const res = tab.value === 'recipe'
      ? await authorRecipes(route.params.id, params)
      : await authorStoreVisits(route.params.id, params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    tabLoading.value = false
  }
}

function switchTab(t) {
  tab.value = t
  loadTab(1)
}

async function openSv(sv) {
  currentSv.value = null
  svVisible.value = true
  try {
    const res = await storeVisitDetail(sv.id)
    currentSv.value = res.data
  } catch (e) {
    svVisible.value = false
  }
}

function openReserve() {
  reserveVisible.value = true
}

function goChat() {
  router.push(`/chat/${profile.value.user.id}?name=${encodeURIComponent(profile.value.user.nickname || '')}`)
}

async function submitReserve() {
  await reserveFormRef.value.validate()
  reserving.value = true
  try {
    await createReservation({
      expertId: profile.value.user.id,
      ...reserveForm
    })
    ElMessage.success('预约已提交，等待达人处理')
    reserveVisible.value = false
  } finally {
    reserving.value = false
  }
}

onMounted(async () => {
  try {
    const p = await authorProfile(route.params.id)
    profile.value = p.data
    await loadTab(1)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.profile-page {
  max-width: var(--fc-page);
  margin: 0 auto;
}

.profile-head {
  display: flex;
  gap: 22px;
  align-items: center;
  background: var(--fc-surface);
  border-radius: var(--fc-r-lg);
  padding: 28px 30px;
  margin-bottom: 22px;
  flex-wrap: wrap;
}

.who {
  flex: 1;
  min-width: 240px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: var(--fc-sp-2-5);
}

.name-row h1 {
  margin: 0;
  font-size: var(--fc-fs-2xl);
}

.intro {
  color: var(--fc-neutral);
  margin: var(--fc-sp-2) 0 var(--fc-sp-1);
  font-size: var(--fc-fs-base);
}

.meta {
  color: var(--fc-muted);
  font-size: var(--fc-fs-sm);
  margin: 0;
}

.head-actions {
  display: flex;
  gap: var(--fc-sp-2-5);
}

.tabs {
  display: flex;
  gap: var(--fc-sp-2);
  margin-bottom: var(--fc-sp-4);
}

.tab {
  padding: 7px 18px;
  border-radius: 999px;
  font-size: var(--fc-fs-base);
  cursor: pointer;
  background: var(--fc-surface);
  color: var(--fc-neutral);
}

.tab.active {
  background: var(--fc-ink);
  color: oklch(97% 0.012 70);
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fc-sp-4);
}

.rc {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-md);
  overflow: hidden;
  display: block;
}

.sv-card {
  cursor: pointer;
}

.thumb {
  height: 150px;
}

.body {
  padding: var(--fc-sp-3) var(--fc-sp-3-5);
}

.body h3 {
  margin: 0 0 var(--fc-sp-1-5);
  font-size: var(--fc-fs-lg);
  line-height: 1.4;
}

.meta2 {
  color: var(--fc-neutral);
  font-size: var(--fc-fs-xs);
}

.pager {
  margin-top: 22px;
  justify-content: center;
}

.sv-cover {
  height: 220px;
  border-radius: var(--fc-r-sm-md);
  overflow: hidden;
  margin-bottom: var(--fc-sp-3);
}

.sv-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fc-sp-3-5);
  color: var(--fc-neutral);
  font-size: var(--fc-fs-sm);
  margin-bottom: var(--fc-sp-2-5);
}

@media (max-width: 800px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
