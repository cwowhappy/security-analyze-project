# 公司信息模块 — 详细技术设计方案

> 本文档定义公司信息模块的完整技术设计方案，涵盖公司列表、公司详情（含基本信息、关联证券、财务报告、基本面分析、历史变更五大 Tab）的前后端详细设计。  
> 版本：v1.0  
> 日期：2026-05-09

---

## 〇、关于基本面分析简版的说明

本文档中的"基本面分析" Tab 为公司详情页内的**简版**呈现，与投研分析独立页面的**完整版**共享同一套后端接口和图表组件，但功能范围更窄：

| 维度 | 简版（本文档） | 完整版（见 `fundamental-analysis-technical-design.md`） |
|------|---------------|--------------------------------------------------------|
| 入口 | 公司详情页 Tab | 顶部导航 → 投研分析独立页面 |
| 布局 | 垂直单栏 | 左右分栏（左侧筛选 + 右侧看板） |
| 图表 | 4个 | 5个（含杜邦分析占位） |
| 搜索/筛选 | ❌ 无 | ✅ 全局搜索、行业/市场筛选 |
| 同行业对比 | ❌ 无 | ✅ 底部同行速览表 |
| 对比池 | ❌ 无 | 阶段B预留 |
| 后端接口 | 共用 `/overview` | `/overview` + `/screen` + `/industry-peers` |

> **组件复用关系**：简版直接引用完整版定义的图表组件（`ProfitabilityChart`、`CostExpenseChart`、`BalanceSheetChart`、`CashFlowChart`），无需重复开发。

---

## 一、模块架构

### 1.1 后端 Package 结构

```
backend/src/main/java/com/example/securityanalyze/company/
├── api/
│   ├── CompanyController.java
│   ├── CompanyListItem.java
│   ├── CompanyListResponse.java
│   ├── CompanyDetailResponse.java
│   └── SecurityItem.java
├── application/
│   └── CompanyService.java
├── domain/
│   ├── Company.java
│   ├── CompanySecurity.java
│   ├── CompanyRepository.java
│   └── CompanySecurityRepository.java
└── infrastructure/
    ├── CompanyRepositoryImpl.java
    └── CompanySecurityRepositoryImpl.java
```

### 1.2 前端目录结构

```
frontend/src/
├── api/company.ts
├── types/company.ts
└── views/company/
    ├── CompanyListView.vue
    ├── CompanyDetailView.vue
    └── components/
        ├── BasicInfoTab.vue           # 基本信息 Tab（可内联）
        ├── SecuritiesTab.vue          # 关联证券 Tab（可内联）
        ├── FinanceReportTab.vue       # 财务报告 Tab（引用 finance/）
        ├── FundamentalAnalysisTab.vue # 基本面分析 Tab（新增）
        └── HistoryTab.vue             # 历史变更 Tab（占位）
```

---

## 二、数据模型

### 2.1 数据库表结构

#### company（公司法人实体）

```sql
CREATE TABLE IF NOT EXISTS company (
    id BIGSERIAL PRIMARY KEY,
    unified_code VARCHAR(50) UNIQUE,              -- 统一社会信用代码（预留）
    company_name VARCHAR(200) NOT NULL,           -- 公司全称
    short_name VARCHAR(100),                      -- 公司简称
    industry VARCHAR(100),                        -- 所属行业
    region VARCHAR(50),                           -- 地区（省份/直辖市）
    establish_date DATE,                          -- 成立日期
    registered_capital DECIMAL(20,4),             -- 注册资本（万元）
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,    -- 逻辑删除标志
    deleted_at TIMESTAMP,                         -- 删除时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_company_unified_code ON company(unified_code);
CREATE INDEX idx_company_name ON company(company_name);
CREATE INDEX idx_company_industry ON company(industry);
CREATE INDEX idx_company_region ON company(region);
CREATE INDEX idx_company_is_deleted ON company(is_deleted);
```

#### company_security（上市证券）

```sql
CREATE TABLE IF NOT EXISTS company_security (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES company(id) ON DELETE CASCADE,
    stock_code VARCHAR(20) NOT NULL UNIQUE,       -- 股票代码，全局唯一
    stock_name VARCHAR(100) NOT NULL,             -- 证券简称
    market VARCHAR(10),                           -- 市场板块：SH / SZ / BJ / HK
    security_type VARCHAR(20),                    -- 证券类型：A股 / B股 / H股
    listing_date DATE,                            -- 在该市场的上市日期
    listing_status VARCHAR(20) DEFAULT 'listed',  -- 上市状态：listed / suspended / delisted
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,    -- 逻辑删除标志
    deleted_at TIMESTAMP,                         -- 删除时间
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cs_stock_code ON company_security(stock_code);
CREATE INDEX idx_cs_company_id ON company_security(company_id);
CREATE INDEX idx_cs_market ON company_security(market);
CREATE INDEX idx_cs_security_type ON company_security(security_type);
CREATE INDEX idx_cs_listing_status ON company_security(listing_status);
CREATE INDEX idx_cs_is_deleted ON company_security(is_deleted);
```

### 2.2 Domain 实体

#### Company.java

```java
package com.example.securityanalyze.company.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("company")
public class Company {

    @Id
    private Long id;

    private String unifiedCode;
    private String companyName;
    private String shortName;
    private String industry;
    private String region;
    private LocalDate establishDate;
    private BigDecimal registeredCapital;

    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### CompanySecurity.java

```java
package com.example.securityanalyze.company.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Table("company_security")
public class CompanySecurity {

    @Id
    private Long id;

    private Long companyId;
    private String stockCode;
    private String stockName;
    private String market;
    private String securityType;
    private LocalDate listingDate;
    private String listingStatus;

    private Boolean isDeleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 三、后端详细设计

### 3.1 Repository 层

#### CompanyRepository.java

```java
public interface CompanyRepository {
    List<Company> findByKeyword(String keyword, int offset, int limit);
    long countByKeyword(String keyword);
    Optional<Company> findById(Long id);
    List<Company> findAllById(List<Long> ids);
    Optional<Company> findByStockCode(String stockCode);
    void deleteById(Long id);
}
```

#### CompanySecurityRepository.java

```java
public interface CompanySecurityRepository {
    List<CompanySecurity> findByCompanyId(Long companyId);
    List<CompanySecurity> findByCompanyIds(List<Long> companyIds);
    Optional<CompanySecurity> findByStockCode(String stockCode);
    List<CompanySecurity> findByKeyword(String keyword, int offset, int limit);
    long countByKeyword(String keyword);
    void deleteByCompanyId(Long companyId);
    void deleteByStockCode(String stockCode);
}
```

#### CompanyRepositoryImpl.java — 核心 SQL

```java
private static final String SELECT_SQL = """
    SELECT id, unified_code, company_name, short_name, industry, region,
           establish_date, registered_capital, is_deleted, deleted_at,
           created_at, updated_at
    FROM company
    WHERE is_deleted = FALSE
    """;

// 按关键词查询（公司名称/简称模糊匹配）
public List<Company> findByKeyword(String keyword, int offset, int limit) {
    String sql = SELECT_SQL;
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("offset", offset);
    params.addValue("limit", limit);

    if (keyword != null && !keyword.isBlank()) {
        sql += " AND (company_name ILIKE :keyword OR short_name ILIKE :keyword)";
        params.addValue("keyword", "%" + keyword.trim() + "%");
    }

    sql += " ORDER BY id ASC LIMIT :limit OFFSET :offset";
    return jdbcTemplate.query(sql, params, ROW_MAPPER);
}

// 统计数量
public long countByKeyword(String keyword) {
    String sql = "SELECT COUNT(*) FROM company WHERE is_deleted = FALSE";
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (keyword != null && !keyword.isBlank()) {
        sql += " AND (company_name ILIKE :keyword OR short_name ILIKE :keyword)";
        params.addValue("keyword", "%" + keyword.trim() + "%");
    }
    Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
    return count != null ? count : 0L;
}

// 按 ID 查询
public Optional<Company> findById(Long id) {
    String sql = SELECT_SQL + " AND id = :id";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("id", id);
    List<Company> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
}

// 批量按 ID 查询
public List<Company> findAllById(List<Long> ids) {
    if (ids == null || ids.isEmpty()) return List.of();
    String sql = SELECT_SQL + " AND id IN (:ids)";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("ids", ids);
    return jdbcTemplate.query(sql, params, ROW_MAPPER);
}

// 按股票代码查询公司（通过 company_security 关联）
public Optional<Company> findByStockCode(String stockCode) {
    String sql = SELECT_SQL + """
        AND id = (
            SELECT company_id FROM company_security
            WHERE stock_code = :stockCode AND is_deleted = FALSE
        )
        """;
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("stockCode", stockCode);
    List<Company> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
}

// 逻辑删除
public void deleteById(Long id) {
    String sql = """
        UPDATE company
        SET is_deleted = TRUE, deleted_at = CURRENT_TIMESTAMP
        WHERE id = :id
        """;
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("id", id);
    jdbcTemplate.update(sql, params);
}
```

#### CompanySecurityRepositoryImpl.java — 核心 SQL

```java
private static final String SELECT_SQL = """
    SELECT id, company_id, stock_code, stock_name, market,
           security_type, listing_date, listing_status,
           is_deleted, deleted_at, created_at, updated_at
    FROM company_security
    WHERE is_deleted = FALSE
    """;

// 按公司 ID 查询证券列表
public List<CompanySecurity> findByCompanyId(Long companyId) {
    String sql = SELECT_SQL + " AND company_id = :companyId ORDER BY stock_code ASC";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("companyId", companyId);
    return jdbcTemplate.query(sql, params, ROW_MAPPER);
}

// 按股票代码精确查询
public Optional<CompanySecurity> findByStockCode(String stockCode) {
    String sql = SELECT_SQL + " AND stock_code = :stockCode";
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("stockCode", stockCode);
    List<CompanySecurity> results = jdbcTemplate.query(sql, params, ROW_MAPPER);
    return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
}

// 按关键词搜索（股票代码精确匹配 / 名称前缀匹配）
public List<CompanySecurity> findByKeyword(String keyword, int offset, int limit) {
    String sql = SELECT_SQL;
    MapSqlParameterSource params = new MapSqlParameterSource();
    params.addValue("offset", offset);
    params.addValue("limit", limit);

    if (keyword != null && !keyword.isBlank()) {
        sql += " AND (stock_code = :keyword OR stock_name ILIKE :prefix)";
        params.addValue("keyword", keyword.trim());
        params.addValue("prefix", keyword.trim() + "%");
    }

    sql += " ORDER BY stock_code ASC LIMIT :limit OFFSET :offset";
    return jdbcTemplate.query(sql, params, ROW_MAPPER);
}

// 统计搜索数量
public long countByKeyword(String keyword) {
    String sql = "SELECT COUNT(*) FROM company_security WHERE is_deleted = FALSE";
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (keyword != null && !keyword.isBlank()) {
        sql += " AND (stock_code = :keyword OR stock_name ILIKE :prefix)";
        params.addValue("keyword", keyword.trim());
        params.addValue("prefix", keyword.trim() + "%");
    }
    Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
    return count != null ? count : 0L;
}
```

### 3.2 Service 层

#### CompanyService.java

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanySecurityRepository companySecurityRepository;
    private final CompanyIndustryMappingRepository companyIndustryMappingRepository;
    private final IndustryCategoryRepository industryCategoryRepository;

    // ========== 公司列表 ==========
    public CompanyListResponse listCompanies(String keyword, int page, int size) {
        int offset = page * size;
        List<CompanySecurity> securities = companySecurityRepository.findByKeyword(keyword, offset, size);
        long total = companySecurityRepository.countByKeyword(keyword);

        // 批量获取公司信息，避免 N+1 查询
        List<Long> companyIds = securities.stream()
                .map(CompanySecurity::getCompanyId)
                .distinct()
                .toList();
        Map<Long, Company> companyMap = companyRepository.findAllById(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, c -> c));

        List<CompanyListItem> items = securities.stream()
                .map(s -> toListItem(s, companyMap.get(s.getCompanyId())))
                .toList();

        CompanyListResponse response = new CompanyListResponse();
        response.setItems(items);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    // ========== 公司详情 ==========
    public Optional<CompanyDetailResponse> getCompanyDetail(String stockCode) {
        Optional<CompanySecurity> securityOpt = companySecurityRepository.findByStockCode(stockCode);
        if (securityOpt.isEmpty()) return Optional.empty();

        CompanySecurity primarySecurity = securityOpt.get();
        Optional<Company> companyOpt = companyRepository.findById(primarySecurity.getCompanyId());
        if (companyOpt.isEmpty()) return Optional.empty();

        Company company = companyOpt.get();
        List<CompanySecurity> securities = companySecurityRepository.findByCompanyId(company.getId());

        return Optional.of(toDetailResponse(company, primarySecurity, securities));
    }

    // ========== 批量查询 ==========
    public List<CompanyListItem> batchQuery(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) return List.of();
        // 限制批量查询数量
        List<String> codes = stockCodes.size() > 50 ? stockCodes.subList(0, 50) : stockCodes;
        // 逐个查询并组装（或优化为 IN 查询）
        return codes.stream()
                .map(code -> companySecurityRepository.findByStockCode(code).orElse(null))
                .filter(Objects::nonNull)
                .map(sec -> {
                    Company company = companyRepository.findById(sec.getCompanyId()).orElse(null);
                    return toListItem(sec, company);
                })
                .toList();
    }

    // ========== 逻辑删除 ==========
    @Transactional
    public void deleteCompany(Long companyId) {
        log.info("逻辑删除公司, companyId={}", companyId);
        companyRepository.deleteById(companyId);
        companySecurityRepository.deleteByCompanyId(companyId);
    }

    // ========== 私有转换方法 ==========
    private CompanyListItem toListItem(CompanySecurity security, Company company) {
        CompanyListItem item = new CompanyListItem();
        item.setStockCode(security.getStockCode());
        item.setStockName(security.getStockName());
        item.setListingDate(security.getListingDate());
        item.setMarket(security.getMarket());
        if (company != null) {
            item.setIndustry(company.getIndustry());
            item.setRegion(company.getRegion());
        }
        return item;
    }

    private CompanyDetailResponse toDetailResponse(Company company, CompanySecurity primarySecurity,
                                                    List<CompanySecurity> securities) {
        CompanyDetailResponse response = new CompanyDetailResponse();
        response.setStockCode(primarySecurity.getStockCode());
        response.setStockName(primarySecurity.getStockName());
        response.setIndustry(company.getIndustry());
        response.setRegion(company.getRegion());
        response.setEstablishDate(company.getEstablishDate());
        response.setRegisteredCapital(company.getRegisteredCapital());
        response.setListingDate(primarySecurity.getListingDate());
        response.setMarket(primarySecurity.getMarket());

        response.setSecurities(securities.stream()
                .map(this::toSecurityItem)
                .toList());

        // 填充多标准行业分类
        response.setIndustries(loadIndustries(company.getId()));
        return response;
    }

    private SecurityItem toSecurityItem(CompanySecurity security) {
        SecurityItem item = new SecurityItem();
        item.setStockCode(security.getStockCode());
        item.setStockName(security.getStockName());
        item.setMarket(security.getMarket());
        item.setSecurityType(security.getSecurityType());
        item.setListingDate(security.getListingDate());
        item.setListingStatus(security.getListingStatus());
        return item;
    }

    private List<CompanyIndustryDto> loadIndustries(Long companyId) {
        List<CompanyIndustryMapping> mappings = companyIndustryMappingRepository.findByCompanyId(companyId);
        List<CompanyIndustryDto> result = new ArrayList<>();
        for (CompanyIndustryMapping mapping : mappings) {
            CompanyIndustryDto dto = new CompanyIndustryDto();
            dto.setStandardCode(mapping.getStandardCode());
            dto.setPrimary(mapping.getPrimary());
            dto.setStandardName(switch (mapping.getStandardCode()) {
                case "SW" -> "申万行业分类";
                case "EM" -> "东方财富行业分类";
                default -> mapping.getStandardCode();
            });
            // 查询一级/二级行业名称...
            result.add(dto);
        }
        return result;
    }
}
```

### 3.3 Controller 与 DTO

#### CompanyController.java

```java
@Slf4j
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<CompanyListResponse> listCompanies(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int[] normalized = PageUtils.normalize(page, size);
        CompanyListResponse response = companyService.listCompanies(
                keyword, normalized[0], normalized[1]);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{stockCode}")
    public ResponseEntity<CompanyDetailResponse> getCompanyDetail(
            @PathVariable String stockCode) {
        return companyService.getCompanyDetail(stockCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/batch")
    public ResponseEntity<List<CompanyListItem>> batchQuery(
            @RequestBody List<String> stockCodes) {
        return ResponseEntity.ok(companyService.batchQuery(stockCodes));
    }
}
```

#### DTO 定义

```java
// CompanyListItem.java
@Data
public class CompanyListItem {
    private String stockCode;
    private String stockName;
    private String industry;
    private String region;
    private LocalDate listingDate;
    private String market;
}

// CompanyListResponse.java
@Data
public class CompanyListResponse {
    private List<CompanyListItem> items;
    private long total;
    private int page;
    private int size;
}

// CompanyDetailResponse.java
@Data
public class CompanyDetailResponse {
    private String stockCode;
    private String stockName;
    private String industry;
    private String region;
    private LocalDate establishDate;
    private BigDecimal registeredCapital;
    private LocalDate listingDate;
    private String market;
    private List<SecurityItem> securities;
    private List<CompanyIndustryDto> industries;
}

// SecurityItem.java
@Data
public class SecurityItem {
    private String stockCode;
    private String stockName;
    private String market;
    private String securityType;
    private LocalDate listingDate;
    private String listingStatus;
}
```

---

## 四、前端详细设计

### 4.1 API 封装

#### `src/api/company.ts`

```typescript
import { client } from './axios'
import type { CompanyListParams, CompanyListResponse, CompanyDetail, CompanyListItem } from '@/types/company'

export async function getCompanyList(params: CompanyListParams = {}): Promise<CompanyListResponse> {
  const response = await client.get('/companies', { params })
  return response.data
}

export async function getCompanyDetail(stockCode: string): Promise<CompanyDetail> {
  const response = await client.get(`/companies/${stockCode}`)
  return response.data
}

export async function getCompaniesByCodes(codes: string[]): Promise<CompanyListItem[]> {
  const response = await client.post('/companies/batch', codes)
  return response.data
}
```

### 4.2 TypeScript 类型定义

#### `src/types/company.ts`

```typescript
export interface SecurityItem {
  stockCode: string
  stockName: string
  market?: string
  securityType?: string
  listingDate?: string
  listingStatus?: string
}

export interface CompanyIndustryDto {
  standardName: string
  standardCode: string
  level1Code?: string
  level1Name?: string
  level2Code?: string
  level2Name?: string
  primary: boolean
}

export interface Company {
  stockCode: string
  stockName: string
  industry?: string
  region?: string
  listingDate?: string
  market?: string
}

export interface CompanyDetail extends Company {
  establishDate?: string
  registeredCapital?: number
  securities?: SecurityItem[]
  industries?: CompanyIndustryDto[]
}

export interface CompanyListParams {
  keyword?: string
  page?: number
  size?: number
}

export interface CompanyListResponse {
  items: Company[]
  total: number
  page: number
  size: number
}
```

### 4.3 公司列表页（`CompanyListView.vue`）

**功能**：
- 分页表格展示公司证券列表
- 关键词搜索（股票代码精确匹配 + 名称前缀匹配）
- 本地「重点关注」功能（`localStorage` 存储，后续可替换为后端接口）
- 点击行跳转公司详情

**核心结构**：

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElTable, ElTableColumn, ElPagination, ElMessage, ElTag } from 'element-plus'
import { getCompanyList, getCompaniesByCodes } from '@/api/company'
import type { Company, CompanyListResponse } from '@/types/company'

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const tableData = ref<Company[]>([])
const total = ref(0)
const page = ref(0)
const size = ref(20)

// 重点关注（本地存储）
const FAVORITE_STORAGE_KEY = 'favorite_companies'
function toggleFavorite(company: Company) {
  const codes = JSON.parse(localStorage.getItem(FAVORITE_STORAGE_KEY) || '[]')
  const idx = codes.indexOf(company.stockCode)
  if (idx > -1) codes.splice(idx, 1)
  else codes.push(company.stockCode)
  localStorage.setItem(FAVORITE_STORAGE_KEY, JSON.stringify(codes))
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getCompanyList({
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value,
    })
    tableData.value = res.items
    total.value = res.total
  } catch (err) {
    ElMessage.error('加载公司列表失败')
  } finally {
    loading.value = false
  }
}

function goToDetail(row: Company) {
  router.push({ name: 'company-detail', params: { stockCode: row.stockCode } })
}

onMounted(fetchList)
</script>

<template>
  <div class="company-list">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <ElInput v-model="keyword" placeholder="输入股票代码或名称" clearable @keyup.enter="fetchList">
        <template #append>
          <ElButton @click="fetchList">搜索</ElButton>
        </template>
      </ElInput>
    </div>

    <!-- 数据表格 -->
    <ElTable :data="tableData" v-loading="loading" @row-click="goToDetail">
      <ElTableColumn prop="stockCode" label="股票代码" width="100" />
      <ElTableColumn prop="stockName" label="公司名称" />
      <ElTableColumn prop="industry" label="所属行业" />
      <ElTableColumn prop="region" label="地区" />
      <ElTableColumn prop="market" label="市场" width="80">
        <template #default="{ row }">
          <ElTag size="small">{{ row.market }}</ElTag>
        </template>
      </ElTableColumn>
      <ElTableColumn label="操作" width="100">
        <template #default="{ row }">
          <ElButton text @click.stop="toggleFavorite(row)">关注</ElButton>
        </template>
      </ElTableColumn>
    </ElTable>

    <!-- 分页 -->
    <ElPagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, sizes, prev, pager, next"
      @change="fetchList"
    />
  </div>
</template>
```

### 4.4 公司详情页（`CompanyDetailView.vue`）

**Tab 结构**：

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElTabs, ElTabPane, ElMessage } from 'element-plus'
import { getCompanyDetail } from '@/api/company'
import type { CompanyDetail } from '@/types/company'
import FinanceReportTab from './finance/FinanceReportTab.vue'
import FundamentalAnalysisTab from './fundamental/FundamentalAnalysisTab.vue'

const route = useRoute()
const router = useRouter()
const stockCode = route.params.stockCode as string
const loading = ref(false)
const company = ref<CompanyDetail | null>(null)
const activeTab = ref('basic')

async function fetchDetail() {
  loading.value = true
  try {
    company.value = await getCompanyDetail(stockCode)
  } catch (err) {
    ElMessage.error('加载公司详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(fetchDetail)
</script>

<template>
  <div class="company-detail" v-loading="loading">
    <!-- 面包屑 -->
    <ElBreadcrumb separator="/">
      <ElBreadcrumbItem :to="{ path: '/' }">首页</ElBreadcrumbItem>
      <ElBreadcrumbItem :to="{ path: '/companies' }">公司信息</ElBreadcrumbItem>
      <ElBreadcrumbItem>{{ company?.stockName || stockCode }}</ElBreadcrumbItem>
    </ElBreadcrumb>

    <!-- 页面标题 -->
    <h2 class="page-title">
      {{ company?.stockName || stockCode }}
      <span class="subtitle">({{ company?.stockCode || stockCode }})</span>
    </h2>

    <!-- Tab 页签 -->
    <ElTabs v-model="activeTab" style="margin-top: 16px">
      <!-- 1. 基本信息 -->
      <ElTabPane label="基本信息" name="basic">
        <div v-if="company" class="info-grid">
          <div class="info-item">
            <span class="label">股票代码</span>
            <span class="value">{{ company.stockCode }}</span>
          </div>
          <div class="info-item">
            <span class="label">公司名称</span>
            <span class="value">{{ company.stockName }}</span>
          </div>
          <div class="info-item">
            <span class="label">所属行业</span>
            <div v-if="company.industries?.length" class="industry-tags">
              <div v-for="ind in company.industries" :key="ind.standardCode + ind.level2Code" class="industry-tag-row">
                <ElTag size="small">{{ ind.standardName }}</ElTag>
                <ElLink type="primary" @click="router.push(`/industries/${encodeURIComponent(ind.level2Code || '')}`)">
                  {{ ind.level2Name || ind.level1Name || '-' }}
                </ElLink>
                <ElTag v-if="!ind.primary" size="small" type="info">次</ElTag>
              </div>
            </div>
            <ElLink v-else-if="company.industry" type="primary">{{ company.industry }}</ElLink>
            <span v-else class="value">-</span>
          </div>
          <div class="info-item">
            <span class="label">地区</span>
            <span class="value">{{ company.region || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">成立日期</span>
            <span class="value">{{ company.establishDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">注册资本</span>
            <span class="value">{{ company.registeredCapital ? company.registeredCapital + ' 万元' : '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">上市日期</span>
            <span class="value">{{ company.listingDate || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="label">市场板块</span>
            <span class="value">{{ company.market || '-' }}</span>
          </div>
        </div>
      </ElTabPane>

      <!-- 2. 关联证券 -->
      <ElTabPane label="关联证券" name="securities">
        <div v-if="company?.securities?.length" class="securities-grid">
          <ElCard v-for="sec in company.securities" :key="sec.stockCode" shadow="hover">
            <template #header>
              <div class="security-header">
                <span class="security-name">{{ sec.stockName }}</span>
                <span class="security-code">({{ sec.stockCode }})</span>
              </div>
            </template>
            <div class="security-info">
              <div class="security-row">
                <span class="security-label">市场板块</span>
                <span class="security-value">{{ sec.market || '-' }}</span>
              </div>
              <div class="security-row">
                <span class="security-label">证券类型</span>
                <span class="security-value">{{ sec.securityType || '-' }}</span>
              </div>
              <div class="security-row">
                <span class="security-label">上市日期</span>
                <span class="security-value">{{ sec.listingDate || '-' }}</span>
              </div>
              <div class="security-row">
                <span class="security-label">上市状态</span>
                <span class="security-value">{{ sec.listingStatus || '-' }}</span>
              </div>
            </div>
          </ElCard>
        </div>
        <ElEmpty v-else description="暂无关联证券数据" />
      </ElTabPane>

      <!-- 3. 财务报告（保持现有功能） -->
      <ElTabPane label="财务报告" name="finance">
        <FinanceReportTab :stock-code="stockCode" />
      </ElTabPane>

      <!-- 4. 基本面分析（新增） -->
      <ElTabPane label="基本面分析" name="fundamental">
        <FundamentalAnalysisTab :stock-code="stockCode" />
      </ElTabPane>

      <!-- 5. 历史变更（占位） -->
      <ElTabPane label="历史变更" name="history">
        <ElEmpty description="历史变更记录开发中" />
      </ElTabPane>
    </ElTabs>
  </div>
</template>

<style scoped>
.company-detail { padding: 8px; }
.page-title {
  font-size: 24px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 16px 0 0;
}
.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 8px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 600px;
}
.info-item {
  display: flex;
  align-items: baseline;
}
.info-item .label {
  color: var(--text-secondary);
  width: 100px;
  flex-shrink: 0;
}
.info-item .value {
  font-weight: 500;
  color: var(--text-primary);
}
.securities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.security-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.security-name { font-weight: 600; font-size: 16px; }
.security-code { color: var(--text-secondary); font-size: 14px; }
.security-info { display: flex; flex-direction: column; gap: 8px; }
.security-row { display: flex; justify-content: space-between; }
.industry-tags { display: flex; flex-direction: column; gap: 4px; }
.industry-tag-row { display: flex; align-items: center; gap: 4px; }
</style>
```

### 4.5 基本面分析 Tab（`FundamentalAnalysisTab.vue`）

**组件定位**：公司详情页内的精简版基本面分析，只展示单公司近5年年报核心指标和趋势图。

**Props**：
```typescript
defineProps<{
  stockCode: string
}>()
```

**核心状态**：
```typescript
const loading = ref(false)
const overview = ref<FundamentalOverview | null>(null)

// 取最新年报数据用于卡片展示
const latestMetric = computed(() => {
  if (!overview.value?.metrics?.length) return null
  return overview.value.metrics[overview.value.metrics.length - 1]
})
```

**布局结构**：

```vue
<template>
  <div class="fundamental-tab">
    <!-- 加载状态 -->
    <ElSkeleton v-if="loading" :rows="3" animated />

    <template v-else-if="overview">
      <!-- 第一层：核心指标卡片区 -->
      <div class="metric-cards">
        <MetricCard
          v-for="card in metricCardConfigs"
          :key="card.key"
          :label="card.label"
          :value="latestMetric?.[card.key]"
          :color="card.color"
          :formatter="card.formatter"
        />
      </div>

      <!-- 第二层：趋势图表区 -->
      <div class="charts-area">
        <ProfitabilityChart :metrics="overview.metrics" />
        <CostExpenseChart :metrics="overview.metrics" />
        <BalanceSheetChart :metrics="overview.metrics" />
        <CashFlowChart :metrics="overview.metrics" />
      </div>

      <!-- 第三层：数据一览表（折叠） -->
      <ElCollapse>
        <ElCollapseItem title="近5年数据一览">
          <FundamentalDataTable :metrics="overview.metrics" />
        </ElCollapseItem>
      </ElCollapse>
    </template>

    <!-- 空态 -->
    <ElEmpty v-else description="暂无年报数据，请检查采集任务是否已完成" />
  </div>
</template>
```

**指标卡片配置**：

```typescript
const metricCardConfigs = [
  { key: 'totalRevenue', label: '营业总收入', color: '#00d4ff', formatter: formatMoney },
  { key: 'parentNetProfit', label: '归母净利润', color: '#67c23a', formatter: formatMoney },
  { key: 'totalAssets', label: '总资产', color: '#ff9500', formatter: formatMoney },
  { key: 'totalEquity', label: '净资产', color: '#f56c6c', formatter: formatMoney },
  { key: 'operatingCashFlow', label: '经营现金流', color: '#9ca3af', formatter: formatMoney },
  { key: 'grossMargin', label: '毛利率', color: '#409eff', formatter: formatPercent },
  { key: 'netMargin', label: '净利率', color: '#67c23a', formatter: formatPercent },
  { key: 'roe', label: 'ROE', color: '#e6a23c', formatter: formatPercent },
]

function formatMoney(val?: number): string {
  if (val == null) return '-'
  const abs = Math.abs(val)
  if (abs >= 1e8) return (val / 1e8).toFixed(2) + ' 亿'
  if (abs >= 1e4) return (val / 1e4).toFixed(2) + ' 万'
  return val.toLocaleString()
}

function formatPercent(val?: number): string {
  if (val == null) return '-'
  return val.toFixed(2) + '%'
}
```

**数据获取**：

```typescript
import { getFundamentalOverview } from '@/api/research'
import type { FundamentalOverview } from '@/types/research'

async function fetchData() {
  loading.value = true
  try {
    overview.value = await getFundamentalOverview(props.stockCode)
  } catch (err) {
    ElMessage.error('加载基本面数据失败')
    console.error(err)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
watch(() => props.stockCode, fetchData)
```

**样式**：

```css
.fundamental-tab { padding: 8px 0; }
.metric-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 24px;
}
.charts-area {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
```

---

## 五、API 接口契约

### 5.1 公司列表

**GET** `/api/companies`

**请求参数**：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|:----:|--------|------|
| keyword | string | 否 | — | 股票代码精确匹配，公司名称前缀匹配 |
| page | integer | 否 | 0 | 页码 |
| size | integer | 否 | 20 | 每页条数，最大 100 |

**响应**：`CompanyListResponse`

```json
{
  "items": [
    {
      "stockCode": "600519",
      "stockName": "贵州茅台",
      "industry": "白酒",
      "region": "贵州",
      "listingDate": "2001-08-27",
      "market": "SH"
    }
  ],
  "total": 1,
  "page": 0,
  "size": 20
}
```

### 5.2 公司详情

**GET** `/api/companies/{stockCode}`

**响应**：`CompanyDetailResponse`

```json
{
  "stockCode": "600519",
  "stockName": "贵州茅台",
  "industry": "白酒",
  "region": "贵州",
  "establishDate": "1999-11-20",
  "registeredCapital": 125619.78,
  "listingDate": "2001-08-27",
  "market": "SH",
  "securities": [
    {
      "stockCode": "600519",
      "stockName": "贵州茅台",
      "market": "SH",
      "securityType": "A股",
      "listingDate": "2001-08-27",
      "listingStatus": "listed"
    }
  ],
  "industries": [
    {
      "standardName": "申万行业分类",
      "standardCode": "SW",
      "level1Code": "C15",
      "level1Name": "食品饮料",
      "level2Code": "C1511",
      "level2Name": "白酒",
      "primary": true
    }
  ]
}
```

### 5.3 批量查询

**POST** `/api/companies/batch`

**请求体**：`List<String>`（股票代码数组，最多 50 个）

```json
["600519", "000001", "000002"]
```

**响应**：`List<CompanyListItem>`

### 5.4 基本面分析（投研模块接口，公司详情页调用）

**GET** `/api/research/fundamental/overview/{stockCode}`

**响应**：`FundamentalOverviewResponse`

```json
{
  "stockCode": "600519",
  "stockName": "贵州茅台",
  "industry": "白酒",
  "market": "SH",
  "metrics": [
    {
      "reportDate": "2023-12-31",
      "reportYear": 2023,
      "totalRevenue": 15054577.44,
      "operateIncome": 15054577.44,
      "operateCost": 1818216.67,
      "parentNetProfit": 7473403.43,
      "grossMargin": 87.92,
      "netMargin": 52.49,
      "roe": 28.64,
      "totalAssets": 27269971.20,
      "totalLiabilities": 4909808.57,
      "totalEquity": 22360162.63,
      "debtRatio": 18.00,
      "operatingCashFlow": 6659312.38,
      "investingCashFlow": -287144.17,
      "financingCashFlow": -3886326.00,
      "endCce": 6907005.18,
      "cashflowProfitRatio": 89.11,
      "saleExpense": 464649.08,
      "manageExpense": 1193668.12,
      "researchExpense": 15720.81,
      "financeExpense": -179168.24,
      "periodExpenseRate": 10.04
    }
  ]
}
```

---

## 六、路由配置

### 6.1 前端路由（`src/router/index.ts`）

```typescript
{
  path: '/companies',
  name: 'company-list',
  component: () => import('@/views/company/CompanyListView.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/companies/:stockCode',
  name: 'company-detail',
  component: () => import('@/views/company/CompanyDetailView.vue'),
  meta: { requiresAuth: true },
},
// 基本面分析独立页面（投研模块）
{
  path: '/research/fundamental',
  name: 'fundamental-analysis',
  component: () => import('@/views/research/FundamentalAnalysisView.vue'),
  meta: { requiresAuth: true },
},
```

### 6.2 顶部导航新增入口

在主导航组件中新增"投研分析"菜单：

```vue
<ElMenuItem index="/research/fundamental">
  <span>投研分析</span>
</ElMenuItem>
```

---

## 七、测试策略

### 7.1 后端测试

**Repository 集成测试**（`CompanyRepositoryImplTest`、`CompanySecurityRepositoryImplTest`）：

```java
@Test
void shouldFindCompanyByStockCode() {
    // 插入 company + company_security
    // 调用 findByStockCode("600519")
    // 断言返回正确的 company 信息
}

@Test
void shouldReturnEmptyWhenCompanyDeleted() {
    // 插入 is_deleted=TRUE 的公司
    // 调用 findById
    // 断言返回 Optional.empty()
}

@Test
void shouldFindSecuritiesByCompanyId() {
    // 插入一家公司 + 2个证券
    // 调用 findByCompanyId
    // 断言返回2条记录，按 stock_code 升序
}
```

**Service 单元测试**（`CompanyServiceTest`）：

```java
@Test
void shouldLoadCompanyWithSecurities() {
    // Mock repository 返回 company + 2 securities
    // 调用 getCompanyDetail("000488")
    // 断言 securities 列表大小为2
}

@Test
void shouldReturn404WhenStockCodeNotFound() {
    // Mock securityRepository 返回 Optional.empty()
    // 调用 getCompanyDetail("999999")
    // 断言返回 Optional.empty()
}
```

**Controller 单元测试**（`CompanyControllerTest`）：

```java
@Test
void shouldReturn200WhenCompanyExists() throws Exception {
    mockMvc.perform(get("/api/companies/600519"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.stockCode").value("600519"));
}

@Test
void shouldReturn404WhenCompanyNotFound() throws Exception {
    mockMvc.perform(get("/api/companies/999999"))
           .andExpect(status().isNotFound());
}
```

### 7.2 前端测试

当前项目尚未配置前端测试框架，以手工验证为主：

- [ ] 公司列表页：搜索、分页、跳转详情正常
- [ ] 公司详情页：五个 Tab 切换正常
- [ ] 基本信息 Tab：字段展示正确，行业标签可点击跳转
- [ ] 关联证券 Tab：证券卡片展示正确
- [ ] 财务报告 Tab：保持现有功能不变
- [ ] 基本面分析 Tab：图表渲染、数据加载、空态展示正常
- [ ] 历史变更 Tab：占位提示正常

---

## 八、实施检查清单

### 数据库
- [ ] 确认 `company` 和 `company_security` 表已包含 `is_deleted` 和 `deleted_at` 字段
- [ ] 确认所有查询 SQL 已追加 `WHERE is_deleted = FALSE`

### 后端
- [ ] `Company.java`、`CompanySecurity.java` 包含逻辑删除字段
- [ ] `CompanyRepositoryImpl`、`CompanySecurityRepositoryImpl` 查询过滤已删除记录
- [ ] `CompanyService` 实现批量查询和逻辑删除方法
- [ ] `CompanyController` 暴露 `/companies/batch` 接口
- [ ] 单元测试覆盖 Repository / Service / Controller 三层

### 前端
- [ ] `src/api/company.ts` 包含 `getCompaniesByCodes`
- [ ] `CompanyListView.vue` 搜索、分页、关注功能正常
- [ ] `CompanyDetailView.vue` 包含五个 Tab（基本信息/关联证券/财务报告/基本面分析/历史变更）
- [ ] `FundamentalAnalysisTab.vue` 实现精简版基本面分析
- [ ] 顶部导航新增"投研分析"入口
- [ ] 路由注册 `/research/fundamental`

### 联调
- [ ] 公司详情页五个 Tab 数据加载正常
- [ ] 基本面分析 Tab 图表渲染正确
- [ ] 无数据场景空态展示正确
- [ ] 关联证券 Tab 展示该公司全部证券
