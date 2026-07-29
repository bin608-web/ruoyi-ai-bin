#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
文本词频词性分析工具

功能：
1. 支持直接输入文本或读取文件进行分词
2. 统计词频并计算排名
3. 标注每个词的词性
4. 支持停用词过滤
5. 支持最小词长度过滤
6. 返回 JSON 格式的分析结果

依赖：
- jieba（中文分词）：pip install jieba
- 标准库：json, argparse, re, sys, os, collections
"""

import json
import re
import sys
import os
import argparse
from collections import Counter, OrderedDict

try:
    import jieba
    import jieba.posseg as pseg
except ImportError as e:
    print(json.dumps({
        "status": "error",
        "message": f"缺少必要的依赖库 jieba，请执行: pip install jieba",
        "output": None
    }, ensure_ascii=False))
    sys.exit(1)


def load_text_from_file(file_path):
    """
    从文件读取文本内容
    
    Args:
        file_path: 文件路径
        
    Returns:
        str: 文件内容
        
    Raises:
        FileNotFoundError: 文件不存在
        UnicodeDecodeError: 文件编码错误
        PermissionError: 无读取权限
    """
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"文件不存在: {file_path}")
    
    if not os.path.isfile(file_path):
        raise ValueError(f"路径不是文件: {file_path}")
    
    if not os.access(file_path, os.R_OK):
        raise PermissionError(f"无权限读取文件: {file_path}")
    
    # 尝试多种编码读取
    encodings = ['utf-8', 'gbk', 'gb2312', 'gb18030', 'latin-1']
    
    for encoding in encodings:
        try:
            with open(file_path, 'r', encoding=encoding) as f:
                content = f.read()
            return content
        except UnicodeDecodeError:
            continue
        except Exception as e:
            raise Exception(f"读取文件失败 ({encoding}): {str(e)}")
    
    raise UnicodeDecodeError(f"无法识别文件编码，已尝试: {', '.join(encodings)}")


def clean_text(text):
    """
    清洗文本：去除多余空白、特殊字符（保留中文、英文、数字）
    
    Args:
        text: 原始文本
        
    Returns:
        str: 清洗后的文本
    """
    if not text or not isinstance(text, str):
        return ""
    
    # 去除HTML标签
    text = re.sub(r'<[^>]+>', '', text)
    
    # 去除URL
    text = re.sub(r'https?://\S+', '', text)
    
    # 去除邮箱地址
    text = re.sub(r'\S+@\S+', '', text)
    
    # 保留中文、英文、数字、常用标点（用于分词边界识别）
    text = re.sub(r'[^\u4e00-\u9fff\w\s，。！？；：、""''（）《》【】…—\-,.!?;:\'"()\[\]{}]', ' ', text)
    
    # 合并多个空白字符
    text = re.sub(r'\s+', ' ', text)
    
    return text.strip()


def parse_stop_words(stop_words_str):
    """
    解析停用词字符串
    
    Args:
        stop_words_str: 逗号分隔的停用词字符串
        
    Returns:
        set: 停用词集合
    """
    if not stop_words_str:
        return set()
    
    stop_words = set()
    for word in stop_words_str.split(','):
        word = word.strip()
        if word:
            stop_words.add(word)
    
    return stop_words


def analyze_text(text, top_n=20, min_word_length=1, stop_words_set=None):
    """
    分析文本词频和词性
    
    Args:
        text: 输入文本
        top_n: 返回词频最高的前N个词
        min_word_length: 最小词长度过滤
        stop_words_set: 停用词集合
        
    Returns:
        dict: 分析结果，包含词频统计和词性标注
    """
    if not text:
        return {
            "total_words": 0,
            "unique_words": 0,
            "word_frequency": [],
            "pos_analysis": []
        }
    
    if stop_words_set is None:
        stop_words_set = set()
    
    # 清洗文本
    cleaned_text = clean_text(text)
    
    if not cleaned_text:
        return {
            "total_words": 0,
            "unique_words": 0,
            "word_frequency": [],
            "pos_analysis": []
        }
    
    # 使用 jieba 进行词性标注和分词
    words_with_pos = []
    try:
        # posseg.cut 返回 (word, flag) 元组
        for word, flag in pseg.cut(cleaned_text):
            word = word.strip()
            if word and len(word) >= min_word_length:
                # 过滤停用词
                if word not in stop_words_set:
                    # 过滤纯标点或空白
                    if not re.match(r'^[\s\W_]+$', word):
                        words_with_pos.append((word, flag))
    except Exception as e:
        raise Exception(f"分词过程出错: {str(e)}")
    
    # 统计词频
    word_list = [item[0] for item in words_with_pos]
    word_counter = Counter(word_list)
    
    # 获取前N个高频词
    top_words = word_counter.most_common(top_n)
    
    # 构建词频结果
    word_frequency = []
    for rank, (word, count) in enumerate(top_words, 1):
        # 获取该词的词性（取第一个出现的词性）
        pos_tags = list(set(flag for w, flag in words_with_pos if w == word))
        
        word_frequency.append({
            "rank": rank,
            "word": word,
            "count": count,
            "frequency": round(count / len(word_list) * 100, 2) if word_list else 0,
            "pos_tags": pos_tags
        })
    
    # 统计词性分布
    pos_counter = Counter(flag for _, flag in words_with_pos)
    total_pos_count = sum(pos_counter.values())
    
    pos_analysis = []
    for pos_tag, count in pos_counter.most_common():
        pos_analysis.append({
            "pos_tag": pos_tag,
            "pos_name": get_pos_name(pos_tag),
            "count": count,
            "percentage": round(count / total_pos_count * 100, 2) if total_pos_count > 0 else 0
        })
    
    return {
        "total_words": len(word_list),
        "unique_words": len(word_counter),
        "word_frequency": word_frequency,
        "pos_analysis": pos_analysis
    }


def get_pos_name(pos_tag):
    """
    获取词性标签的中文名称
    
    Args:
        pos_tag: jieba 词性标签
        
    Returns:
        str: 词性中文名称
    """
    pos_map = {
        'a': '形容词',
        'ad': '副形词',
        'ag': '形语素',
        'an': '名形词',
        'b': '区别词',
        'c': '连词',
        'd': '副词',
        'df': '不要',
        'dg': '副语素',
        'e': '叹词',
        'f': '方位词',
        'g': '语素',
        'h': '前接成分',
        'i': '成语',
        'j': '简称略语',
        'k': '后接成分',
        'l': '习用语',
        'm': '数词',
        'mg': '数语素',
        'mq': '数量词',
        'n': '名词',
        'ng': '名语素',
        'nr': '人名',
        'nrfg': '人名',
        'nrt': '人名',
        'ns': '地名',
        'nt': '机构团体',
        'nz': '其他专名',
        'o': '拟声词',
        'p': '介词',
        'q': '量词',
        'r': '代词',
        'rg': '代语素',
        'rr': '人称代词',
        'rz': '指示代词',
        's': '处所词',
        't': '时间词',
        'tg': '时语素',
        'u': '助词',
        'ud': '得',
        'ug': '过',
        'uj': '的',
        'ul': '连词',
        'uv': '地',
        'uz': '着',
        'v': '动词',
        'vd': '副动词',
        'vg': '动语素',
        'vn': '名动词',
        'vq': '趋向动词',
        'x': '非语素字',
        'y': '语气词',
        'z': '状态词',
        'zg': '状态词',
        'eng': '英文',
        'num': '数字',
        'un': '未知词性',
    }
    
    return pos_map.get(pos_tag, f'未知({pos_tag})')


# ========== 主执行逻辑 ==========
if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="文本词频词性分析工具 - 分析文本的词频和词性分布",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  python text_analysis.py --text "我爱北京天安门，天安门上太阳升"
  python text_analysis.py --file_path ./document.txt --top_n 30
  python text_analysis.py --text "分析这段文本" --min_word_length 2 --stop_words "的,了,在"
        """
    )
    
    parser.add_argument(
        '--text',
        type=str,
        default=None,
        help='要分析的文本内容（与 --file_path 二选一）'
    )
    
    parser.add_argument(
        '--file_path',
        type=str,
        default=None,
        help='要分析的文本文件路径（与 --text 二选一）'
    )
    
    parser.add_argument(
        '--top_n',
        type=int,
        default=20,
        help='返回词频最高的前N个词，默认20'
    )
    
    parser.add_argument(
        '--min_word_length',
        type=int,
        default=1,
        help='过滤掉长度小于此值的词，默认1'
    )
    
    parser.add_argument(
        '--stop_words',
        type=str,
        default="的,了,在,是,我,你,他,她,它,们,这,那,有,不,和,就,都,也,要,会,可,很,还,去,能,对,没,人,个,上,下,来,到,说,想,看,把,被,让,从,给,向,与,及,或,但,而,如,若,虽,然,则,之,其,所,为,以,于,因,由,此,该,各,每,某,何,哪,怎,吗,呢,吧,啊,哦,嗯,哈,呀,哇,呵,嘿,哎,哼,喂,嘻,啦,哟,噢,噶,咔,咚,叮,咚,吱,嘎",
        help='逗号分隔的停用词列表'
    )
    
    try:
        args = parser.parse_args()
        
        # 参数验证
        if not args.text and not args.file_path:
            print(json.dumps({
                "status": "error",
                "message": "必须提供 --text 或 --file_path 参数之一",
                "output": None
            }, ensure_ascii=False))
            sys.exit(1)
        
        if args.text and args.file_path:
            print(json.dumps({
                "status": "error",
                "message": "--text 和 --file_path 不能同时提供，请选择其中一个",
                "output": None
            }, ensure_ascii=False))
            sys.exit(1)
        
        if args.top_n < 1:
            print(json.dumps({
                "status": "error",
                "message": f"--top_n 必须大于0，当前值: {args.top_n}",
                "output": None
            }, ensure_ascii=False))
            sys.exit(1)
        
        if args.min_word_length < 1:
            print(json.dumps({
                "status": "error",
                "message": f"--min_word_length 必须大于0，当前值: {args.min_word_length}",
                "output": None
            }, ensure_ascii=False))
            sys.exit(1)
        
        # 获取文本内容
        input_text = ""
        source_info = ""
        
        if args.text:
            input_text = args.text
            source_info = "直接输入"
        elif args.file_path:
            try:
                input_text = load_text_from_file(args.file_path)
                source_info = f"文件: {args.file_path}"
            except FileNotFoundError as e:
                print(json.dumps({
                    "status": "error",
                    "message": str(e),
                    "output": None
                }, ensure_ascii=False))
                sys.exit(1)
            except PermissionError as e:
                print(json.dumps({
                    "status": "error",
                    "message": str(e),
                    "output": None
                }, ensure_ascii=False))
                sys.exit(1)
            except UnicodeDecodeError as e:
                print(json.dumps({
                    "status": "error",
                    "message": str(e),
                    "output": None
                }, ensure_ascii=False))
                sys.exit(1)
            except Exception as e:
                print(json.dumps({
                    "status": "error",
                    "message": f"读取文件失败: {str(e)}",
                    "output": None
                }, ensure_ascii=False))
                sys.exit(1)
        
        # 检查文本是否为空
        if not input_text or not input_text.strip():
            print(json.dumps({
                "status": "error",
                "message": "输入文本为空，无法进行分析",
                "output": None
            }, ensure_ascii=False))
            sys.exit(1)
        
        # 解析停用词
        stop_words_set = parse_stop_words(args.stop_words)
        
        # 执行分析
        analysis_result = analyze_text(
            text=input_text,
            top_n=args.top_n,
            min_word_length=args.min_word_length,
            stop_words_set=stop_words_set
        )
        
        # 构建输出结果
        output = {
            "source": source_info,
            "text_length": len(input_text),
            "text_preview": input_text[:200] + ("..." if len(input_text) > 200 else ""),
            "parameters": {
                "top_n": args.top_n,
                "min_word_length": args.min_word_length,
                "stop_words_count": len(stop_words_set)
            },
            "analysis": analysis_result
        }
        
        # 输出结果
        print(json.dumps({
            "status": "success",
            "message": f"分析完成，共处理 {analysis_result['total_words']} 个词，发现 {analysis_result['unique_words']} 个不重复词",
            "output": output
        }, ensure_ascii=False, indent=2))
        
    except Exception as e:
        print(json.dumps({
            "status": "error",
            "message": f"程序执行异常: {str(e)}",
            "output": None
        }, ensure_ascii=False))
        sys.exit(1)