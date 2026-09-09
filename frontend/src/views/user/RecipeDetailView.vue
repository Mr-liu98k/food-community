<template>
  <div v-loading="loading" class="detail-page">
    <template v-if="detail">
      <router-link to="/recipes" class="back">← 返回菜谱列表</router-link>

      <!-- 封面条 -->
      <div class="hero">
        <RecipeCover :src="detail.recipe.cover" :title="detail.recipe.title" :seed="detail.recipe.id" />
        <div class="hero-shade">
          <span class="cat-tag">{{ detail.recipe.categoryName }}</span>
          <h1>{{ detail.recipe.title }}</h1>
        </div>
      </div>

      <div class="layout">
        <!-- 主栏 -->
        <article class="main">
          <div class="stats-line">
            <span class="author" @click="goAuthor">
              <el-avatar :size="34" :src="detail.recipe.authorAvatar || undefined">
                {{ detail.recipe.authorNickname?.charAt(0) }}
              </el-avatar>
              <span class="who">
                <b>{{ detail.recipe.authorNickname }}</b>
                <i>发布于 {{ detail.recipe.createTime }}</i>
              </span>
            </span>
            <span class="nums">
              <button class="like-btn" :class="{ on: liked }" @click="toggleLike">👍 {{ likesCount }}</button>
              <button class="fav-btn" :class="{ on: favorited }" @click="toggleFavorite">
                {{ favorited ? '★ 已收藏' : '☆ 收藏' }}
              </button>
            </span>
          </div>

          <section v-if="detail.recipe.description" class="card">
            <h2>关于这道菜</h2>
            <MarkdownContent :content="detail.recipe.description" />
          </section>

          <section class="card">
            <h2>食材清单</h2>
            <ul class="ings">
              <li v-for="ing in detail.ingredients" :key="ing.id">
                <span class="ing-name">{{ ing.name }}</span>
                <span class="ing-amount">{{ ing.amount }}</span>
              </li>
            </ul>
          </section>

          <section class="card">
            <h2>制作步骤</h2>
            <ol class="steps">
              <li v-for="(step, i) in detail.steps" :key="step.id" class="step">
                <span class="step-no">{{ i + 1 }}</span>
                <div class="step-text"><MarkdownContent :content="step.content" /></div>
              </li>
            </ol>
          </section>

          <!-- 评论区：楼中楼 -->
          <CommentSection :recipe-id="detail.recipe.id" />
        </article>

        <!-- 作者卡 -->
        <aside class="side">
          <div class="author-card">
            <el-avatar :size="56" :src="detail.recipe.authorAvatar || undefined">
              {{ detail.recipe.authorNickname?.charAt(0) }}
            </el-avatar>
            <b class="name" @click="goAuthor">{{ detail.recipe.authorNickname }}</b>
            <p class="intro">看看 TA 的主页，发现更多私房菜谱。</p>
            <el-button type="primary" plain round @click="goAuthor">进入 TA 的主页</el-button>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { recipeDetail } from '@/api/recipe'
import { likeRecipe, unlikeRecipe, favoriteRecipe, unfavoriteRecipe } from '@/api/interaction'
import { useUserStore } from '@/stores/user'
import RecipeCover from '@/components/RecipeCover.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const detail = ref(null)
const loading = ref(true)
const liked = ref(false)
const favorited = ref(false)
const likesCount = ref(0)
const interactBusy = ref(false)

onMounted(async () => {
  try {
    const res = await recipeDetail(route.params.id)
    detail.value = res.data
    liked.value = res.data.liked
    favorited.value = res.data.favorited
    likesCount.value = res.data.recipe.likesCount
  } finally {
    loading.value = false
  }
})

function requireLogin() {
  if (!store.isLogin) {
    ElMessage.warning('请先登录')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}

async function toggleLike() {
  if (!requireLogin()) return
  if (interactBusy.value) return
  interactBusy.value = true
  try {
    if (liked.value) {
      await unlikeRecipe(detail.value.recipe.id)
      liked.value = false
      likesCount.value = Math.max(0, likesCount.value - 1)
    } else {
      await likeRecipe(detail.value.recipe.id)
      liked.value = true
      likesCount.value += 1
    }
  } finally {
    interactBusy.value = false
  }
}

async function toggleFavorite() {
  if (!requireLogin()) return
  if (interactBusy.value) return
  interactBusy.value = true
  try {
    if (favorited.value) {
      await unfavoriteRecipe(detail.value.recipe.id)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await favoriteRecipe(detail.value.recipe.id)
      favorited.value = true
      ElMessage.success('收藏成功，可在「我的收藏」查看')
    }
  } finally {
    interactBusy.value = false
  }
}

function goAuthor() {
  if (detail.value) {
    router.push(`/user/${detail.value.recipe.userId}`)
  }
}
</script>

<style scoped>
.back {
  display: inline-block;
  margin-bottom: 14px;
  color: var(--fc-neutral);
  font-size: 14px;
}

.back:hover {
  color: var(--fc-accent);
}

.hero {
  position: relative;
  height: 320px;
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 22px;
}

.hero-shade {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 26px 30px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.55), transparent 60%);
}

.cat-tag {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.9);
  color: var(--fc-accent);
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  margin-bottom: 8px;
}

.hero h1 {
  margin: 0;
  color: #fff;
  font-size: 34px;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.3);
}

.layout {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 22px;
  align-items: start;
}

.stats-line {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.author {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.who {
  display: flex;
  flex-direction: column;
}

.who b {
  font-size: 15px;
}

.who i {
  font-style: normal;
  font-size: 12px;
  color: var(--fc-neutral);
}

.nums {
  display: flex;
  gap: 10px;
  color: var(--fc-neutral);
  font-size: 14px;
}

.nums button {
  border: 1px solid var(--fc-rule);
  background: oklch(99% 0.006 70);
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 13px;
  cursor: pointer;
  color: var(--fc-ink);
  transition: all 0.2s;
}

.nums button:hover {
  border-color: var(--fc-accent);
}

.like-btn.on {
  background: var(--fc-accent-soft);
  border-color: var(--fc-accent);
  color: var(--fc-accent);
}

.fav-btn.on {
  border-color: #d4a017;
  color: #b8860b;
}

.card {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 12px;
  padding: 20px 22px;
  margin-bottom: 16px;
}

.card h2 {
  margin: 0 0 12px;
  font-size: 19px;
}

.ings {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.ings li {
  display: flex;
  justify-content: space-between;
  padding: 9px 2px;
  border-bottom: 1px dashed var(--fc-rule);
  font-size: 14px;
}

.ings li:last-child {
  border-bottom: none;
}

.ing-name {
  color: var(--fc-ink);
}

.ing-amount {
  color: var(--fc-accent);
  font-weight: 600;
}

.steps {
  list-style: none;
  margin: 0;
  padding: 0;
  counter-reset: step;
}

.step {
  display: flex;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid var(--fc-rule);
}

.step:last-child {
  border-bottom: none;
}

.step-no {
  flex-shrink: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--fc-ink);
  color: oklch(97% 0.012 70);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.step-text {
  flex: 1;
}

.side {
  position: sticky;
  top: 76px;
}

.author-card {
  background: var(--fc-surface);
  border-radius: 12px;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.author-card .name {
  cursor: pointer;
  font-size: 17px;
}

.author-card .name:hover {
  color: var(--fc-accent);
}

.intro {
  color: var(--fc-neutral);
  font-size: 13px;
  line-height: 1.6;
  margin: 0 0 8px;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .side {
    position: static;
  }
}
</style>
