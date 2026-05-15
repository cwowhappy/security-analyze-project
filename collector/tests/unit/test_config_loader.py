import pytest
from data_collector.core.config.field_mapping_config import FieldMappingConfigLoader


class TestFieldMappingConfigLoader:
    def test_load_valid_config(self, tmp_path):
        config_dir = tmp_path / "mappings"
        config_dir.mkdir()
        config_file = config_dir / "stock_basic.yaml"
        config_file.write_text("""
task_type: stock_basic
ttl_hours: 12
sources:
  - name: akshare
    adapter: stock_basic_akshare_adapter
    priority: 1
    field_mapping:
      - api_field: "代码"
        db_field: "stock_code"
        converter: "str"
        null_policy: "skip"
""")
        loader = FieldMappingConfigLoader(str(config_dir))
        config = loader.load("stock_basic")
        assert config.task_type == "stock_basic"
        assert config.ttl_hours == 12
        assert len(config.sources) == 1
        assert config.sources[0].name == "akshare"
        assert config.sources[0].field_mapping[0].db_field == "stock_code"
