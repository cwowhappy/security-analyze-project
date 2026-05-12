# API 响应录制 Fixture

此目录用于存放采集脚本的外部 API 响应录制数据，支持离线回归测试。

## 使用方式

```python
import json
from pathlib import Path

FIXTURE_DIR = Path(__file__).parent

def load_fixture(name: str) -> dict:
    with open(FIXTURE_DIR / f"{name}.json", "r", encoding="utf-8") as f:
        return json.load(f)
```

## 录制脚本示例

```python
import akshare as ak
import json

df = ak.stock_info_a_code_name()
df.head(10).to_json("tests/fixtures/stock_info_a_code_name.json", orient="records", force_ascii=False)
```

## 当前 Fixture 清单

- [ ] `stock_info_a_code_name.json` — akshare 全量股票列表
- [ ] `stock_profile_cninfo_000001.json` — 单条公司详情
- [ ] `stock_basic_tushare_000001.json` — tushare 股票基础信息
