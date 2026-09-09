<template>
  <div class="fav-page">
    <div class="head">
      <div>
        <h1>我的收藏</h1>
        <p>共 {{ total }} 道收藏的菜谱</p>
      </div>
      <el-button round @click="router.push('/recipes')">去逛菜谱</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="rows.length" class="grid">
        <div v-for="(r, i) in rows" :key="r.id" class="fc card-link">
          <router-link :to="`/recipe/${r.id}`" class="link-area">
            <div class="thumb"><RecipeCover :src="r.cover" :title="r.title" :seed="r.id" /></div>
            <div class="body">
              <h3>{{ r.title }}</h3>
              <div class="meta">{{ r.categoryName }} · {{ r.authorNickname }}</div>
            </div>
          </router-link>
          <div class="foot">
            <span class="views">{{ r.views }} 次浏览</span>
            <el-button link type="danger" @click="unfav(r)">取消收藏</el-button>
          </div>
        </div>
      </div>
      <el-empty v-else-if="!loading" description="还没有收藏，看到喜欢的菜谱点 ★ 收藏" />
    </div>

    <el-pagination v-if="total > pageSize" class="pager" background layout="prev, pager, next" :total="total"
                   :page-size="pageSize" :current-page="page" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { myFavorites, unfavoriteRecipe } from '@/api/interaction'
import RecipeCover from '@/components/RecipeCover.vue'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 12
const loading = ref(false)

async function load(p) {
  page.value = p
  loading.value = true
  try {
    const res = await myFavorites({ page: p, size: pageSize })
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function unfav(row) {
  await unfavoriteRecipe(row.id)
  ElMessage.success('已取消收藏')
  load(page.value)
}

onMounted(() => load(1))
</script>

<style scoped>
.fav-page {
  max-width: 920px;
  margin: 0 auto;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 18px;
}

.head h1 {
  margin: 0 0 4px;
  font-size: 28px;
}

.head p {
  margin: 0;
  color: var(--fc-neutral);
  font-size: 13px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.fc {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.link-area {
  flex: 1;
}

.thumb {
  height: 150px;
}

.body {
  padding: 12px 14px 4px;
}

.body h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.meta {
  color: var(--fc-neutral);
  font-size: 12px;
}

.foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 12px 10px;
}

.views {
  color: var(--fc-muted);
  font-size: 12px;
}

.pager {
  margin-top: 22px;
  justify-content: center;
}

@media (max-width: 800px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
