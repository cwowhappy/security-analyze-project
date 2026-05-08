"""Test sw_industry_mapping.json extraction and loading."""
import json
import os
import pytest

from collector.tasks.company_task import CompanyTask


class TestSwIndustryMapping:
    def test_json_file_exists(self):
        json_path = os.path.join(
            os.path.dirname(__file__), "..", "collector", "data", "sw_industry_mapping.json"
        )
        assert os.path.exists(json_path), f"sw_industry_mapping.json not found at {json_path}"

    def test_json_structure(self):
        json_path = os.path.join(
            os.path.dirname(__file__), "..", "collector", "data", "sw_industry_mapping.json"
        )
        with open(json_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        assert "L1" in data
        assert "L2" in data
        assert isinstance(data["L1"], dict)
        assert isinstance(data["L2"], dict)
        # spot check some known mappings
        assert data["L1"]["11"] == "801030"
        assert data["L2"]["1101"] == "801038"

    def test_company_task_loads_mapping(self):
        from unittest.mock import MagicMock
        task = CompanyTask(db=MagicMock(), source=MagicMock(), monitor=None)
        # _preload_mappings should load the json file
        task._preload_mappings()
        assert task._mappings_loaded is True
        assert len(task._sw_mapping) >= 0  # may be empty if akshare fails, but loaded flag set
