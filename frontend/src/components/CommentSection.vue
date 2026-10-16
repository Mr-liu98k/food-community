<template>
  <section class="card comment-sec">
    <h2>评论 <span class="cnt">({{ total }})</span></h2>

    <!-- 发表框 -->
    <div class="composer">
      <el-avatar :size="36" :src="store.user?.avatar || undefined">{{ store.user?.nickname?.charAt(0) }}</el-avatar>
      <div class="composer-main">
        <el-input v-model="draft" type="textarea" :rows="2" :placeholder="replyingTo ? `回复 @${replyingTo.authorNickname}：` : '说说这道菜…'"
                  maxlength="500" show-word-limit />
        <div class="composer-actions">
          <el-button v-if="replyingTo" size="small" @click="cancelReply">取消回复</el-button>
          <el-button size="small" type="primary" :loading="submitting" @click="submitComment">发表</el-button>
        </div>
      </div>
    </div>

    <!-- 评论列表 -->
    <div v-loading="loading">
      <template v-if="roots.length">
        <div v-for="root in roots" :key="root.id" class="comment-item">
          <el-avatar :size="38" :src="root.authorAvatar || undefined">{{ root.authorNickname?.charAt(0) }}</el-avatar>
          <div class="comment-body">
            <div class="head">
              <b class="name">{{ root.authorNickname }}</b>
              <span class="time">{{ root.createTime }}</span>
            </div>
            <p class="text">{{ root.content }}</p>
            <div class="ops">
              <a class="op" @click="startReply(root)">回复</a>
              <a v-if="canDelete(root)" class="op danger" @click="del(root)">删除</a>
            </div>

            <!-- 楼中楼子评论（平铺） -->
            <div v-if="root.children.length" class="children">
              <div v-for="child in root.children" :key="child.id" class="child">
                <el-avatar :size="26" :src="child.authorAvatar || undefined">{{ child.authorNickname?.charAt(0) }}</el-avatar>
                <div class="child-main">
                  <div class="child-head">
                    <b class="name">{{ child.authorNickname }}</b>
                    <span v-if="child.replyNickname" class="reply-to">回复 @{{ child.replyNickname }}</span>
                    <span class="time">{{ child.createTime }}</span>
                  </div>
                  <p class="text">{{ child.content }}</p>
                  <div class="ops">
                    <a class="op" @click="startReply(child)">回复</a>
                    <a v-if="canDelete(child)" class="op danger" @click="del(child)">删除</a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-pagination v-if="total > pageSize" small background layout="prev, pager, next" :total="total"
                       :page-size="pageSize" :current-page="page" @current-change="load" class="pager" />
      </template>
      <el-empty v-else-if="!loading" description="还没有评论，来抢沙发" :image-size="70" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { recipeComments, postComment, removeComment } from '@/api/interaction'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  recipeId: { type: [String, Number], required: true }
})

const store = useUserStore()
const roots = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const draft = ref('')
const replyingTo = ref(null)
const submitting = ref(false)

async function load(p) {
  page.value = p
  loading.value = true
  try {
    const res = await recipeComments(props.recipeId, { page: p, size: pageSize })
    roots.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function startReply(comment) {
  replyingTo.value = comment
}

function cancelReply() {
  replyingTo.value = null
  draft.value = ''
}

async function submitComment() {
  if (!draft.value.trim()) return ElMessage.warning('评论内容不能为空')
  submitting.value = true
  try {
    await postComment(props.recipeId, {
      content: draft.value.trim(),
      parentId: replyingTo.value ? replyingTo.value.id : null
    })
    ElMessage.success('评论成功')
    draft.value = ''
    replyingTo.value = null
    load(1)
  } finally {
    submitting.value = false
  }
}

function canDelete(comment) {
  return store.isAdmin || comment.userId === store.user?.id
}

async function del(comment) {
  await ElMessageBox.confirm('删除后将连同其下所有回复一并删除，确定吗？', '删除评论', { type: 'warning' })
  await removeComment(comment.id)
  ElMessage.success('已删除')
  load(page.value)
}

onMounted(() => load(1))
</script>

<style scoped>
.comment-sec h2 {
  margin: 0 0 var(--fc-sp-4);
  font-size: var(--fc-fs-xl);
}

.cnt {
  color: var(--fc-neutral);
  font-size: var(--fc-fs-base);
  font-weight: 400;
}

.composer {
  display: flex;
  gap: var(--fc-sp-3);
  margin-bottom: var(--fc-sp-5);
}

.composer-main {
  flex: 1;
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--fc-sp-2);
  margin-top: var(--fc-sp-2);
}

.comment-item {
  display: flex;
  gap: var(--fc-sp-3);
  padding: var(--fc-sp-4) 0;
  border-top: 1px solid var(--fc-rule);
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.head {
  display: flex;
  align-items: baseline;
  gap: var(--fc-sp-2-5);
}

.name {
  font-size: var(--fc-fs-base);
}

.time {
  font-size: var(--fc-fs-xs);
  color: var(--fc-neutral);
}

.text {
  margin: var(--fc-sp-1-5) 0;
  font-size: var(--fc-fs-base);
  line-height: 1.7;
  word-break: break-word;
}

.ops {
  display: flex;
  gap: var(--fc-sp-3-5);
}

.op {
  font-size: var(--fc-fs-xs);
  color: var(--fc-neutral);
  cursor: pointer;
}

.op:hover {
  color: var(--fc-accent);
}

.op.danger:hover {
  color: var(--fc-danger);
}

.children {
  margin-top: var(--fc-sp-2-5);
  background: oklch(95% 0.012 70);
  border-radius: var(--fc-r-sm);
  padding: var(--fc-sp-1-5) var(--fc-sp-3-5);
}

.child {
  display: flex;
  gap: var(--fc-sp-2-5);
  padding: var(--fc-sp-2-5) 0;
  border-bottom: 1px dashed var(--fc-rule);
}

.child:last-child {
  border-bottom: none;
}

.child-main {
  flex: 1;
  min-width: 0;
}

.child-head {
  display: flex;
  align-items: baseline;
  gap: var(--fc-sp-2);
  flex-wrap: wrap;
}

.child-head .name {
  font-size: var(--fc-fs-sm);
}

.reply-to {
  font-size: var(--fc-fs-xs);
  color: var(--fc-accent);
}

.pager {
  margin-top: var(--fc-sp-3-5);
  justify-content: center;
}
</style>
