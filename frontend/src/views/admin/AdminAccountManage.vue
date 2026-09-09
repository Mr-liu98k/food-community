<template>
  <div>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="按账号 / 昵称搜索" clearable class="search"
                @keyup.enter="load(1)" @clear="load(1)">
        <template #append><el-button @click="load(1)">搜索</el-button></template>
      </el-input>
      <el-button type="primary" @click="openCreate">
        + 新增{{ mode === 'admins' ? '管理员' : '用户' }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="用户" min-width="180">
        <template #default="{ row }">
          <div class="who">
            <el-avatar :size="28" :src="row.avatar || undefined">{{ row.nickname?.charAt(0) }}</el-avatar>
            <div>
              <div>{{ row.nickname }}</div>
              <div class="acc">{{ row.username }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="mode === 'users'" label="探店达人" width="110">
        <template #default="{ row }">
          <el-switch :model-value="row.isExpert" @change="(v) => toggleExpert(row, v)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="editNickname(row)">改昵称</el-button>
          <el-button link type="warning" @click="resetPwd(row)">重置密码</el-button>
          <el-button link type="danger" :disabled="row.id === myId" :title="row.id === myId ? '不能删除自己' : ''"
                      @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无数据" />

    <el-pagination v-if="total > query.size" class="pager" background layout="prev, pager, next"
                   :total="total" :page-size="query.size" :current-page="query.page" @current-change="load" />

    <!-- 新增 -->
    <el-dialog v-model="createVisible" :title="`新增${mode === 'admins' ? '管理员' : '用户'}`" width="440px">
      <el-form ref="createRef" :model="createForm" :rules="createRules" label-width="80px">
        <el-form-item label="账号" prop="username">
          <el-input v-model="createForm.username" maxlength="20" placeholder="4-20 位" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password maxlength="32" placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" maxlength="20" placeholder="留空默认同账号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adminUserPage, adminCreateUser, adminSetUserExpert, adminUpdateUserNickname,
  adminResetUserPassword, adminRemoveUser,
  adminAdminPage, adminCreateAdmin, adminUpdateAdminNickname,
  adminResetAdminPassword, adminRemoveAdmin
} from '@/api/admin'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  mode: { type: String, default: 'users' } // 'users' 普通用户 | 'admins' 管理员
})

const store = useUserStore()
const myId = computed(() => store.user?.id)

const isAdmins = computed(() => props.mode === 'admins')
const apiMap = computed(() =>
  isAdmins.value
    ? {
        page: adminAdminPage, create: adminCreateAdmin, nickname: adminUpdateAdminNickname,
        password: adminResetAdminPassword, remove: adminRemoveAdmin
      }
    : {
        page: adminUserPage, create: adminCreateUser, nickname: adminUpdateUserNickname,
        password: adminResetUserPassword, remove: adminRemoveUser
      }
)

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 10, keyword: '' })
const createVisible = ref(false)
const saving = ref(false)
const createRef = ref()
const createForm = reactive({ username: '', password: '', nickname: '' })
const createRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '请输入至少 6 位密码', trigger: 'blur' }]
}

async function load(p) {
  query.page = p
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    const res = await apiMap.value.page(params)
    rows.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(createForm, { username: '', password: '', nickname: '' })
  createVisible.value = true
}

async function submitCreate() {
  await createRef.value.validate()
  saving.value = true
  try {
    await apiMap.value.create({ ...createForm })
    ElMessage.success('创建成功')
    createVisible.value = false
    load(1)
  } finally {
    saving.value = false
  }
}

async function toggleExpert(row, val) {
  const action = val ? '设为' : '取消'
  await ElMessageBox.confirm(`确定${action}「${row.nickname}」的探店达人资格吗？`, '达人设置', { type: 'warning' })
  try {
    await adminSetUserExpert(row.id, val)
    ElMessage.success(`已${action}达人`)
    row.isExpert = val
  } catch (e) {
    row.isExpert = !val
  }
}

async function editNickname(row) {
  const { value } = await ElMessageBox.prompt('输入新的昵称', '编辑昵称', {
    inputValue: row.nickname,
    inputValidator: (v) => (v && v.trim() ? true : '昵称不能为空')
  })
  await apiMap.value.nickname(row.id, value.trim())
  ElMessage.success('已更新')
  load(query.page)
}

async function resetPwd(row) {
  const { value } = await ElMessageBox.prompt(`为「${row.nickname}」设置新密码（6-32 位）`, '重置密码', {
    inputType: 'password',
    inputValidator: (v) => (v && v.trim().length >= 6 ? true : '密码至少 6 位')
  })
  await apiMap.value.password(row.id, value.trim())
  ElMessage.success('密码已重置')
}

async function del(row) {
  await ElMessageBox.confirm(
    `确定删除${isAdmins.value ? '管理员' : '用户'}「${row.nickname}」吗？存在业务数据时将无法删除。`,
    '删除确认',
    { type: 'warning' }
  )
  await apiMap.value.remove(row.id)
  ElMessage.success('已删除')
  load(query.page)
}

onMounted(() => load(1))
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}

.search {
  width: 260px;
}

.who {
  display: flex;
  align-items: center;
  gap: 10px;
}

.acc {
  font-size: 12px;
  color: var(--fc-neutral);
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
