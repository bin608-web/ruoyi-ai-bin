---
name: data-processing
description: 数据处理技能，支持 CSV、Excel、JSON 等数据格式的转换、清洗和分析。当用户需要处理数据文件、进行数据转换或数据分析时使用此技能。
license: Proprietary
compatibility: Requires Python 3.8+ and pandas library
metadata:
  author: ruoyi-team
  version: "1.0.0"
  category: data
  tags:
    - csv
    - excel
    - json
    - data-transformation
    - data-cleaning
---

# 数据处理技能

## 功能概述

本技能提供以下能力：
- CSV、Excel、JSON 文件格式转换
- 数据清洗（去重、填充空值、格式标准化）
- 数据聚合和统计分析
- 数据过滤和排序

## 使用方法

### 1. 数据格式转换

```
将 CSV 文件转换为 Excel 格式
将 JSON 数据转换为 CSV 格式
将 Excel 文件转换为 JSON 格式
```

### 2. 数据清洗

```
清理数据中的空值
去除重复记录
标准化日期格式
```

### 3. 数据分析

```
统计销售数据的平均值和总和
按类别分组统计
生成数据报告
```

## 支持的格式

- **输入格式**: CSV, Excel (.xlsx, .xls), JSON, XML
- **输出格式**: CSV, Excel, JSON, HTML

## 注意事项

- 大文件处理可能需要较长时间
- 建议先预览数据再进行批量操作
- 敏感数据请注意隐私保护
