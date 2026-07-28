#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
技能示例：文本总结
描述：对长文本进行自动总结，提取关键信息
生成时间：2026-06-02
"""

import sys
import json

def main(input_data):
    """
    技能主函数
    
    Args:
        input_data: 输入数据（字符串或 JSON 格式）
    
    Returns:
        处理结果（JSON 格式字符串）
    """
    try:
        # 解析输入
        if isinstance(input_data, str):
            try:
                input_json = json.loads(input_data)
            except json.JSONDecodeError:
                input_json = {"text": input_data}
        else:
            input_json = input_data
        
        # 获取输入文本
        text = input_json.get("text", "")
        
        if not text:
            return json.dumps({
                "status": "error",
                "message": "输入文本不能为空"
            }, ensure_ascii=False)
        
        # 简单的文本总结逻辑（示例）
        # 实际使用时可以替换为调用 LLM 进行总结
        
        # 1. 按句子分割
        sentences = text.replace('。', '。\n').replace('！', '！\n').replace('？', '？\n').split('\n')
        sentences = [s.strip() for s in sentences if s.strip()]
        
        # 2. 提取关键句子（示例：取前 3 句）
        summary_sentences = sentences[:min(3, len(sentences))]
        
        # 3. 生成总结
        summary = " ".join(summary_sentences)
        
        # 4. 计算压缩率
        compression_rate = round(len(summary) / len(text) * 100, 2) if text else 0
        
        result = {
            "status": "success",
            "message": "总结成功",
            "original_length": len(text),
            "summary_length": len(summary),
            "compression_rate": f"{compression_rate}%",
            "summary": summary,
            "sentence_count": len(summary_sentences)
        }
        
        return json.dumps(result, ensure_ascii=False, indent=2)
        
    except Exception as e:
        error_result = {
            "status": "error",
            "message": str(e)
        }
        return json.dumps(error_result, ensure_ascii=False)

if __name__ == "__main__":
    # 从命令行参数获取输入
    if len(sys.argv) > 1:
        input_text = " ".join(sys.argv[1:])
    else:
        input_text = ""
    
    result = main(input_text)
    print(result)
