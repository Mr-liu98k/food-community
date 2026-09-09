<template>
  <div class="home">
    <!-- 轮播（管理员配置，关联已通过菜谱） -->
    <el-carousel v-if="bannerList.length" height="300px" class="carousel" indicator-position="outside">
      <el-carousel-item v-for="b in bannerList" :key="b.id">
        <router-link :to="`/recipe/${b.recipeId}`" class="banner-slide">
          <img v-if="b.image" :src="b.image" :alt="b.recipeTitle" />
          <div v-else class="banner-fallback" :style="{ background: bannerGradient(b.id) }">
            <span>{{ b.recipeTitle }}</span>
          </div>
          <div class="banner-mask"><span>{{ b.recipeTitle }}</span></div>
        </router-link>
      </el-carousel-item>
    </el-carousel>

    <!-- 最新公告 -->
    <router-link v-if="latestNotice" to="/notices" class="notice-strip">
      <span class="flag">公告</span>
      <span class="title">{{ latestNotice.title }}</span>
      <span class="go">查看全部 →</span>
    </router-link>

    <!-- 今日灵感：编辑榜首（非对称：大标题区 + 大图区） -->
    <section v-if="featured" class="featured">
      <div class="featured-copy">
        <span class="kicker">今日灵感 · 大家都在看</span>
        <h1 class="featured-title">
          <router-link :to="`/recipe/${featured.id}`">{{ featured.title }}</router-link>
        </h1>
        <p class="featured-desc">{{ featured.description || '来自社区的美食笔记，点击查看完整做法。' }}</p>
        <div class="featured-meta">
          <span class="author">
            <el-avatar :size="24" :src="featured.authorAvatar || undefined">{{ featured.authorNickname?.charAt(0) }}</el-avatar>
            {{ featured.authorNickname }}
          </span>
          <span>·</span>
          <span>{{ featured.categoryName }}</span>
          <span>·</span>
          <span>{{ featured.views }} 次浏览</span>
        </div>
        <router-link :to="`/recipe/${featured.id}`" class="read-btn">查看做法 →</router-link>
      </div>
      <router-link :to="`/recipe/${featured.id}`" class="featured-cover card-link">
        <RecipeCover :src="featured.cover" :title="featured.title" :seed="featured.id" />
      </router-link>
    </section>

    <!-- 按分类逛逛 -->
    <section class="block">
      <div class="block-head">
        <h2>按分类逛逛</h2>
      </div>
      <div class="cats">
        <router-link v-for="c in categories" :key="c.id" :to="{ path: '/recipes', query: { categoryId: c.id } }"
                     class="cat-chip">
          <span class="cat-dot" :style="{ background: dotColor(c.id) }"></span>
          {{ c.name }}
        </router-link>
      </div>
    </section>

    <!-- 热门菜谱网格 -->
    <section class="block">
      <div class="block-head">
        <h2>社区正在做</h2>
        <router-link to="/recipes" class="more">查看全部 →</router-link>
      </div>
      <div v-if="hots.length" class="recipe-grid">
        <router-link v-for="(r, i) in hots" :key="r.id" :to="`/recipe/${r.id}`" class="recipe-card card-link">
          <div class="thumb"><RecipeCover :src="r.cover" :title="r.title" :seed="r.id" /></div>
          <div class="info">
            <h3>{{ r.title }}</h3>
            <p class="meta">{{ r.categoryName }} · {{ r.views }} 次浏览</p>
          </div>
        </router-link>
      </div>
      <el-empty v-else description="还没有已发布的菜谱" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { getCategories } from '@/api/category'
import { hotRecipes } from '@/api/recipe'
import { banners } from '@/api/interaction'
import { noticeLatest } from '@/api/notice'
import RecipeCover from '@/components/RecipeCover.vue'

const categories = ref([])
const hots = ref([])
const bannerList = ref([])
const latestNotice = ref(null)
const featured = computed(() => hots.value[0] || null)
const displayHots = computed(() => hots.value.slice(0, 8))

const DOT_COLORS = ['#e8590c', '#d97706', '#b45309', '#c2410c', '#ea580c', '#9a3412', '#f97316', '#a16207']
const dotColor = (id) => DOT_COLORS[Number(id) % DOT_COLORS.length]
const bannerGradient = (id) => DOT_COLORS[Number(id) % DOT_COLORS.length]

onMounted(async () => {
  const [c, h, b, n] = await Promise.all([getCategories(), hotRecipes(9), banners(), noticeLatest(1)])
  categories.value = c.data
  hots.value = h.data
  bannerList.value = b.data
  latestNotice.value = n.data?.[0] || null
})
</script>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  gap: 36px;
}

/* 最新公告条 */
.notice-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  background: oklch(96% 0.03 70);
  border: 1px solid oklch(90% 0.05 60);
  border-radius: 10px;
  padding: 10px 16px;
  font-size: 14px;
  color: var(--fc-ink);
}

.notice-strip .flag {
  background: var(--fc-accent);
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  flex-shrink: 0;
}

.notice-strip .title {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-strip .go {
  color: var(--fc-accent);
  font-size: 12px;
  flex-shrink: 0;
}

/* 轮播 */
.carousel {
  border-radius: 16px;
  overflow: hidden;
}

.banner-slide {
  position: relative;
  display: block;
  height: 100%;
  border-radius: 16px;
  overflow: hidden;
}

.banner-slide img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.banner-fallback {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--fc-font-display);
  font-size: 30px;
  color: rgba(255, 255, 255, 0.92);
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.banner-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 26px;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.5), transparent);
  color: #fff;
  font-family: var(--fc-font-display);
  font-size: 22px;
  font-weight: 600;
}

/* 编辑榜首：左右非对称布局 */
.featured {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 32px;
  align-items: center;
  background: var(--fc-surface);
  border-radius: 18px;
  overflow: hidden;
  padding: 36px 36px 36px 44px;
}

.featured-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 14px;
}

.kicker {
  font-size: 13px;
  letter-spacing: 0.08em;
  color: var(--fc-accent);
  font-weight: 600;
}

.featured-title {
  font-size: 34px;
  line-height: 1.2;
  margin: 0;
}

.featured-desc {
  color: var(--fc-neutral);
  line-height: 1.7;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--fc-muted);
  font-size: 13px;
}

.author {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--fc-ink);
}

.read-btn {
  margin-top: 6px;
  display: inline-block;
  background: var(--fc-ink);
  color: oklch(97% 0.012 70);
  padding: 10px 22px;
  border-radius: 999px;
  font-size: 14px;
  transition: background 0.2s;
}

.read-btn:hover {
  background: var(--fc-accent);
}

.featured-cover {
  height: 320px;
  border-radius: 14px;
  overflow: hidden;
  display: block;
}

/* 区块通用 */
.block {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.block-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.block-head h2 {
  font-size: 22px;
  margin: 0;
}

.more {
  font-size: 14px;
  color: var(--fc-accent);
}

/* 分类 chips */
.cats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.cat-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--fc-surface);
  border-radius: 999px;
  font-size: 14px;
  color: var(--fc-ink);
  border: 1px solid transparent;
  transition: all 0.2s;
}

.cat-chip:hover {
  border-color: var(--fc-accent);
  color: var(--fc-accent);
}

.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

/* 热门网格：2 列大卡 */
.recipe-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.recipe-card {
  background: oklch(99% 0.006 70);
  border: 1px solid var(--fc-rule);
  border-radius: 14px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.thumb {
  height: 180px;
}

.info {
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info h3 {
  margin: 0;
  font-size: 17px;
  line-height: 1.4;
}

.meta {
  margin: 0;
  color: var(--fc-neutral);
  font-size: 13px;
}

@media (max-width: 800px) {
  .featured {
    grid-template-columns: 1fr;
    padding: 24px;
  }

  .featured-cover {
    height: 220px;
  }

  .recipe-grid {
    grid-template-columns: 1fr;
  }
}
</style>
