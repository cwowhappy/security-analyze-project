"""采集任务领域模型单元测试。"""

import pytest

from data_collector.core.domain.collection_task import CollectionTask, TaskStatus


class TestCollectionTask:
    """采集任务领域模型测试。"""

    def test_should_create_task_with_valid_data(self) -> None:
        task = CollectionTask(task_type="stock_full")
        assert task.task_type == "stock_full"
        assert task.status == TaskStatus.PENDING.value

    def test_should_raise_error_when_task_type_empty(self) -> None:
        with pytest.raises(ValueError, match="任务类型 task_type 不能为空"):
            CollectionTask(task_type="")

    def test_should_serialize_task_params(self) -> None:
        task = CollectionTask(
            task_type="stock_single",
            task_params={"stock_code": "000001"},
        )
        data = task.to_dict()
        assert isinstance(data["task_params"], str)

    def test_should_deserialize_task_params(self) -> None:
        task = CollectionTask.from_dict({
            "task_type": "stock_single",
            "task_params": '{"stock_code": "000001"}',
            "status": "running",
        })
        assert task.task_params == {"stock_code": "000001"}
        assert task.status == "running"


def test_collection_task_mode_and_source_priority():
    from data_collector.core.domain.collection_task import CollectionTask
    task = CollectionTask(
        task_type="stock_basic",
        mode="full",
        source_priority=["akshare", "tushare"],
    )
    assert task.mode == "full"
    assert task.source_priority == ["akshare", "tushare"]
    d = task.to_dict()
    assert d["mode"] == "full"
    assert d["source_priority"] == '["akshare", "tushare"]'
