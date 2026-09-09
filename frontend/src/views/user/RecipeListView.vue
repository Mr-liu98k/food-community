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
            <router-link v-for="(r, i) in rows" :key="r.id" :to="`/recipe/${r.id}`" class="rc card-link">
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
  font-size: 30px;
  margin: 0 0 6px;
}

.page-title p {
  color: var(--fc-neutral);
  margin: 0 0 20px;
}

.layout {
  display: grid;
  grid-template-columns: 180px 1fr;
  gap: 24px;
  align-items: start;
}

.side {
  background: var(--fc-surface);
  border-radius: 12px;
  padding: 14px;
  position: sticky;
  top: 76px;
}

.side-title {
  font-size: 13px;
  color: var(--fc-muted);
  letter-spacing: 0.08em;
  margin-bottom: 8px;
}

.cat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 9px 12px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: var(--fc-ink);
  transition: all 0.15s;
}

.cat-item:hover {
  background: oklch(90% 0.02 60 / 0.4);
}

.cat-item.active {
  background: var(--fc-ink);
  color: oklch(97% 0.012 70);
}

.tick {
  font-size: 12px;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.search {
  width: 260px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.rc {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.thumb {
  height: 150px;
}

.body {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.body h3 {
  margin: 0;
  font-size: 16px;
  line-height: 1.4;
  flex: 1;
}

.byline {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--fc-neutral);
}

.tags {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.tag {
  color: var(--fc-accent);
  background: var(--fc-accent-soft);
  padding: 2px 8px;
  border-radius: 999px;
}

.view {
  color: var(--fc-neutral);
}

.pager {
  margin-top: 24px;
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
