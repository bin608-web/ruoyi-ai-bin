#!/usr/bin/env python3
"""
数据格式转换脚本
支持 CSV、Excel、JSON 之间的相互转换
"""

import pandas as pd
import json
import sys
from pathlib import Path

def convert_csv_to_excel(csv_file: str, excel_file: str):
    """将 CSV 转换为 Excel"""
    df = pd.read_csv(csv_file)
    df.to_excel(excel_file, index=False)
    print(f"成功转换：{csv_file} -> {excel_file}")

def convert_excel_to_csv(excel_file: str, csv_file: str):
    """将 Excel 转换为 CSV"""
    df = pd.read_excel(excel_file)
    df.to_csv(csv_file, index=False, encoding='utf-8-sig')
    print(f"成功转换：{excel_file} -> {csv_file}")

def convert_json_to_csv(json_file: str, csv_file: str):
    """将 JSON 转换为 CSV"""
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    df = pd.DataFrame(data)
    df.to_csv(csv_file, index=False, encoding='utf-8-sig')
    print(f"成功转换：{json_file} -> {csv_file}")

def convert_csv_to_json(csv_file: str, json_file: str):
    """将 CSV 转换为 JSON"""
    df = pd.read_csv(csv_file)
    data = df.to_dict('records')
    
    with open(json_file, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"成功转换：{csv_file} -> {json_file}")

def clean_data(input_file: str, output_file: str):
    """数据清洗：去重、填充空值"""
    ext = Path(input_file).suffix.lower()
    
    if ext == '.csv':
        df = pd.read_csv(input_file)
    elif ext in ['.xlsx', '.xls']:
        df = pd.read_excel(input_file)
    elif ext == '.json':
        with open(input_file, 'r', encoding='utf-8') as f:
            df = pd.DataFrame(json.load(f))
    else:
        raise ValueError(f"不支持的文件格式：{ext}")
    
    # 去重
    df = df.drop_duplicates()
    
    # 填充空值（用空字符串）
    df = df.fillna('')
    
    # 保存
    if output_file.endswith('.csv'):
        df.to_csv(output_file, index=False, encoding='utf-8-sig')
    elif output_file.endswith(('.xlsx', '.xls')):
        df.to_excel(output_file, index=False)
    elif output_file.endswith('.json'):
        df.to_json(output_file, force_ascii=False, indent=2, orient='records')
    
    print(f"数据清洗完成：{input_file} -> {output_file}")
    print(f"原始行数：{len(pd.read_csv(input_file) if input_file.endswith('.csv') else pd.read_excel(input_file))}")
    print(f"清洗后行数：{len(df)}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("用法:")
        print("  python data_convert.py csv2excel <输入.csv> <输出.xlsx>")
        print("  python data_convert.py excel2csv <输入.xlsx> <输出.csv>")
        print("  python data_convert.py json2csv <输入.json> <输出.csv>")
        print("  python data_convert.py csv2json <输入.csv> <输出.json>")
        print("  python data_convert.py clean <输入文件> <输出文件>")
        sys.exit(1)
    
    command = sys.argv[1]
    input_file = sys.argv[2]
    output_file = sys.argv[3] if len(sys.argv) > 3 else None
    
    if command == "csv2excel" and output_file:
        convert_csv_to_excel(input_file, output_file)
    elif command == "excel2csv" and output_file:
        convert_excel_to_csv(input_file, output_file)
    elif command == "json2csv" and output_file:
        convert_json_to_csv(input_file, output_file)
    elif command == "csv2json" and output_file:
        convert_csv_to_json(input_file, output_file)
    elif command == "clean" and output_file:
        clean_data(input_file, output_file)
    else:
        print("错误：参数不正确")
        sys.exit(1)
