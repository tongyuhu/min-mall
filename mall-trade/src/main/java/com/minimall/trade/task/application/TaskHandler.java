package com.minimall.trade.task.application;

public interface TaskHandler {
  String type();

  TaskResult execute(TaskLease lease);
}
