---
name: document-analysis
description: 文档分析技能，支持 PDF、Word、Excel 等文档的文本提取、内容分析和信息抽取。当用户需要读取文档内容、提取关键信息或分析文档结构时使用此技能。
license: Proprietary
compatibility: Requires Python 3.8+, pymupdf, python-docx, openpyxl
metadata:
  author: ruoyi-team
  version: "1.0.0"
  category: document
  tags:
    - pdf
    - word
    - excel
    - text-extraction
    - document-analysis
---

# 文档分析技能

## 功能概述

本技能提供以下能力：
- PDF 文档文本提取
- Word 文档内容解析
- Excel 数据读取
- 文档结构分析
- 关键信息抽取

## 使用场景

### 1. PDF 文档处理

```
提取 PDF 文件的所有文本内容
从 PDF 中提取表格数据
统计 PDF 文件的页数和字数
```

### 2. Word 文档分析

```
读取 Word 文档内容
提取 Word 中的标题和段落
统计 Word 文档的字数和段落数
```

### 3. Excel 数据分析

```
读取 Excel 文件的所有工作表
提取 Excel 中的特定列数据
统计 Excel 数据的汇总信息
```

## 支持的格式

- **PDF**: .pdf (支持文本型和扫描型)
- **Word**: .doc, .docx
- **Excel**: .xls, .xlsx
- **文本**: .txt, .md

## 输出格式

- 纯文本 (.txt)
- Markdown (.md)
- JSON (.json)
- Excel (.xlsx)

## 注意事项

- 扫描型 PDF 需要 OCR 支持
- 大文档处理可能需要较长时间
- 加密文档需要密码才能读取

## 示例

### 提取 PDF 内容

```
输入：report.pdf
输出：提取的文本内容或 Markdown 格式
```

### 分析 Word 文档

```
输入：document.docx
输出：文档结构、标题列表、字数统计
```

### 读取 Excel 数据

```
输入：data.xlsx, sheet="Sheet1"
输出：表格数据（JSON 或 CSV 格式）
```
