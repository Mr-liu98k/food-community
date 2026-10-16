<template>
  <div class="recipe-list-page">
    <div class="page-title">
      <h1>{{ currentCat ? currentCat.name : '全部菜谱' }}</h1>
      <p>看看大家最近都在做什么好吃的</p>
    </div>

    <div class="layout">
      <!-- 左：分类导航 -->
      <aside class="side">
        <div class="side-title">分类</div>
        <div class="cat-nav">
          <div class="cat-item" :class="{ active: !query.categoryId }" @click="pick(null)">全部菜谱</div>
          <div v-for="c in categories" :key="c.id" class="cat-item" :class="{ active: query.categoryId == c.id }"
               @click="pick(c.id)">
            <span>{{ c.name }}</span>
            <span v-if="query.categoryId == c.id" class="tick">✓</span>
          </div>
        </div>
      </aside>

      <!-- 右：搜索 + 卡片流 -->
      <section class="main">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="搜索菜谱标题…" clearable class="search"
                    @keyup.enter="load(1)" @clear="load(1)">
            <template #append>
              <el-button @click="load(1)">搜索</el-button>
            </template>
          </el-input>
        </div>

        <div v-loading="loading">
          <div v-if="rows.length" class="grid">
            <router-link v-for="r in rows" :key="r.id" :to="`/recipe/${r.id}`" class="rc card-link">
              <div class="thumb"><RecipeCover :src="r.cover" :title="r.title" :seed="r.id" /></div>
              <div class="body">
                <h3>{{ r.title }}</h3>
                <div class="byline">
                  <el-avatar :size="20" :src="r.authorAvatar || undefined">{{ r.authorNickname?.charAt(0) }}</el-avatar>
                  <span>{{ r.authorNickname }}</span>
                </div>
                <div class="tags">
                  <span class="tag">{{ r.categoryName }}</span>
                  <span class="view">{{ r.views }} 次浏览</span>
                </div>
              </div>
            </router-link>
          </div>
          <el-empty v-else description="这个分类下还没有菜谱" />
        </div>

        <el-pagination v-if="total > pageSize" background layout="prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="query.page" @current-change="load" class="pager" />
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategories } from '@/api/category'
import { pageRecipes } from '@/api/recipe'
import RecipeCover from '@/components/RecipeCover.vue'

const route = useRoute()
const router = useRouter()

const categories = ref([])
const rows = ref([])
const total = ref(0)
const pageSize = 12
const loading = ref(false)

const query = reactive({ page: 1, categoryId: null, keyword: '' })
const currentCat = ref(null)

watch(
  () => route.query,
  () => { syncFromRoute() },
  { immediate: true }
)

function syncFromRoute() {
  query.categoryId = route.query.categoryId ? Number(route.query.categoryId) : null
  load(1)
}

async function load(page) {
  query.page = page
  loading.value = true
  try {
    const params = { page: query.page, size: pageSize }
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.keyword) params.keyword = query.keyword
    const res = await pageRecipes(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
    currentCat.value = categories.value.find((c) => c.id === query.categoryId) || null
  } finally {
    loading.value = false
  }
}

function pick(id) {
  router.push({ path: '/recipes', query: id ? { categoryId: id } : {} })
}

onMounted(async () => {
  const res = await getCategories()
  categories.value = res.data
  syncFromRoute()
})
</script>

<style scoped>
.page-title h1 {
  font-size: var(--fc-fs-2xl);
  margin: 0 0 var(--fc-sp-1);
}

.page-title p {
  color: var(--fc-neutral);
  margin: 0 0 var(--fc-sp-5);
  font-size: var(--fc-fs-base);
}

.layout {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: var(--fc-sp-6);
  align-items: start;
}

/* 分类侧栏：卡片化 + 吸顶 */
.side {
  background: var(--fc-surface-2);
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-md);
  padding: var(--fc-sp-3);
  position: sticky;
  top: 84px;
  box-shadow: var(--fc-sh-xs);
}

.side-title {
  font-size: var(--fc-fs-xs);
  color: var(--fc-muted);
  letter-spacing: 0.08em;
  margin-bottom: var(--fc-sp-2);
  padding-left: var(--fc-sp-2);
}

.cat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fc-sp-2) var(--fc-sp-3);
  border-radius: var(--fc-r-sm);
  cursor: pointer;
  font-size: var(--fc-fs-base);
  color: var(--fc-ink);
  transition: background var(--fc-dur-fast) var(--fc-ease-standard),
              color var(--fc-dur-fast) var(--fc-ease-standard);
}

.cat-item + .cat-item {
  margin-top: var(--fc-sp-0-5);
}

.cat-item:hover {
  background: var(--fc-surface);
}

.cat-item.active {
  background: var(--fc-ink);
  color: var(--fc-ink-invert);
  font-weight: 600;
}

.tick {
  font-size: var(--fc-fs-xs);
}

.toolbar {
  margin-bottom: var(--fc-sp-4);
  display: flex;
  justify-content: flex-end;
}

.search {
  width: 260px;
  max-width: 100%;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fc-sp-4);
}

.rc {
  background: var(--fc-surface-2);
  border: 1px solid var(--fc-rule);
  border-radius: var(--fc-r-md);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.thumb {
  height: 150px;
  flex-shrink: 0;
}

.body {
  padding: var(--fc-sp-3) var(--fc-sp-4) var(--fc-sp-4);
  display: flex;
  flex-direction: column;
  gap: var(--fc-sp-2);
  flex: 1;
}

/* 标题最多 2 行，保证同排卡片底部信息对齐 */
.body h3 {
  margin: 0;
  font-size: var(--fc-fs-lg);
  line-height: var(--fc-lh-snug);
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.byline {
  display: flex;
  align-items: center;
  gap: var(--fc-sp-2);
  font-size: var(--fc-fs-sm);
  color: var(--fc-neutral);
}

.tags {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--fc-fs-xs);
}

.tag {
  color: var(--fc-accent);
  background: var(--fc-accent-soft);
  padding: var(--fc-sp-0-5) var(--fc-sp-2);
  border-radius: var(--fc-r-full);
  font-weight: 500;
}

.view {
  color: var(--fc-neutral);
}

.pager {
  margin-top: var(--fc-sp-6);
  justify-content: center;
}

@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .side {
    position: static;
  }

  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
