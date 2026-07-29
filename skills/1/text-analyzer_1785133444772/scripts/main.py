import json
import re
import logging
import sys
from collections import Counter
from typing import Dict, List, Any, Optional

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)


class TextAnalyzer:
    """文本分析器类，提供字数统计、关键词提取和情感分析功能"""

    def __init__(self):
        """初始化文本分析器，加载停用词表和情感词典"""
        # 中文停用词表（常见停用词）
        self.stop_words = {
            '的', '了', '在', '是', '我', '有', '和', '就', '不', '人', '都', '一',
            '一个', '上', '也', '很', '到', '说', '要', '去', '你', '会', '着',
            '没有', '看', '好', '自己', '这', '他', '她', '它', '们', '那', '些',
            '什么', '怎么', '如何', '为什么', '可以', '这个', '那个', '还是', '但是',
            '因为', '所以', '如果', '虽然', '而且', '或者', '然后', '已经', '正在',
            '把', '被', '从', '以', '对', '与', '等', '及', '为', '之', '将',
            '吗', '呢', '吧', '啊', '哦', '嗯', '呀', '哈', '嘛', '哇', '哎',
            '太', '更', '最', '很', '非常', '特别', '比较', '相当', '有点',
            '能', '能够', '应该', '必须', '需要', '可能', '可以', '会', '要',
            '是', '不', '没', '有', '无', '非', '莫', '勿', '未', '别',
            '和', '与', '及', '以及', '并', '而', '或', '或者', '且', '但',
            'a', 'an', 'the', 'and', 'or', 'but', 'in', 'on', 'at', 'to',
            'for', 'of', 'with', 'by', 'from', 'is', 'are', 'was', 'were',
            'be', 'been', 'being', 'have', 'has', 'had', 'do', 'does', 'did',
            'will', 'would', 'could', 'should', 'may', 'might', 'can', 'shall',
            'i', 'you', 'he', 'she', 'it', 'we', 'they', 'me', 'him', 'her',
            'us', 'them', 'my', 'your', 'his', 'its', 'our', 'their', 'mine',
            'yours', 'hers', 'ours', 'theirs', 'this', 'that', 'these', 'those'
        }

        # 情感词典（正面和负面词汇）
        self.positive_words = {
            '好', '优秀', '出色', '完美', '精彩', '棒', '赞', '喜欢', '爱',
            '快乐', '幸福', '满意', '成功', '积极', '乐观', '美好', '美丽',
            '开心', '高兴', '兴奋', '感动', '温暖', '舒适', '方便', '实用',
            'good', 'great', 'excellent', 'wonderful', 'fantastic', 'amazing',
            'love', 'happy', 'beautiful', 'nice', 'best', 'perfect', 'awesome',
            'outstanding', 'superb', 'brilliant', 'positive', 'pleased',
            'delightful', 'enjoyable', 'satisfying', 'impressive'
        }

        self.negative_words = {
            '差', '糟糕', '坏', '失败', '失望', '讨厌', '恨', '悲伤', '痛苦',
            '消极', '悲观', '丑陋', '难', '困难', '麻烦', '问题', '错误',
            '生气', '愤怒', '恐惧', '担心', '焦虑', '无聊', '累', '烦',
            'bad', 'terrible', 'awful', 'horrible', 'worst', 'poor', 'hate',
            'sad', 'angry', 'disappointed', 'negative', 'ugly', 'difficult',
            'problem', 'error', 'fail', 'failure', 'boring', 'annoying',
            'frustrating', 'unpleasant', 'disgusting', 'terrible'
        }

        # 程度副词权重
        self.intensifiers = {
            '非常': 2.0, '特别': 2.0, '极其': 2.5, '十分': 2.0, '异常': 2.0,
            '太': 1.5, '很': 1.5, '相当': 1.5, '比较': 1.2, '有点': 0.8,
            '稍微': 0.7, '略微': 0.7, '些微': 0.7, '极度': 2.5, '格外': 2.0,
            'very': 2.0, 'extremely': 2.5, 'quite': 1.5, 'rather': 1.3,
            'somewhat': 0.8, 'slightly': 0.7, 'incredibly': 2.5
        }

        # 否定词
        self.negation_words = {
            '不', '没', '无', '非', '莫', '未', '别', '不要', '没有',
            'not', 'no', 'never', 'neither', 'nor', 'nothing', 'none'
        }

    def count_characters(self, text: str) -> Dict[str, int]:
        """
        统计文本字符数

        Args:
            text: 输入文本

        Returns:
            包含各类字符统计的字典
        """
        if not text:
            return {
                "total": 0,
                "chinese": 0,
                "english": 0,
                "digits": 0,
                "spaces": 0,
                "punctuation": 0,
                "others": 0
            }

        stats = {
            "total": len(text),
            "chinese": 0,
            "english": 0,
            "digits": 0,
            "spaces": 0,
            "punctuation": 0,
            "others": 0
        }

        for char in text:
            if '\u4e00' <= char <= '\u9fff' or '\u3400' <= char <= '\u4dbf':
                stats["chinese"] += 1
            elif char.isalpha():
                stats["english"] += 1
            elif char.isdigit():
                stats["digits"] += 1
            elif char.isspace():
                stats["spaces"] += 1
            elif char in '，。！？；：""''（）【】《》、,.!?;:""''()[]{}<>':
                stats["punctuation"] += 1
            else:
                stats["others"] += 1

        return stats

    def count_words(self, text: str) -> Dict[str, int]:
        """
        统计文本词数

        Args:
            text: 输入文本

        Returns:
            包含词数统计的字典
        """
        if not text:
            return {"total_words": 0, "unique_words": 0, "chinese_words": 0, "english_words": 0}

        # 提取中文词汇（使用简单的分词方法）
        chinese_chars = re.findall(r'[\u4e00-\u9fff]+', text)
        chinese_words = []
        for chars in chinese_chars:
            # 简单的中文分词：按2-4个字符切分
            for i in range(len(chars)):
                for j in range(i+1, min(i+5, len(chars)+1)):
                    word = chars[i:j]
                    if len(word) >= 1:
                        chinese_words.append(word)

        # 提取英文词汇
        english_words = re.findall(r'[a-zA-Z]+', text.lower())

        all_words = chinese_words + english_words
        unique_words = set(all_words)

        return {
            "total_words": len(all_words),
            "unique_words": len(unique_words),
            "chinese_words": len(chinese_words),
            "english_words": len(english_words)
        }

    def extract_keywords(self, text: str, top_n: int = 10) -> List[Dict[str, Any]]:
        """
        提取文本关键词

        Args:
            text: 输入文本
            top_n: 返回的关键词数量

        Returns:
            关键词列表，包含词和频率
        """
        if not text:
            return []

        # 提取中文词汇
        chinese_chars = re.findall(r'[\u4e00-\u9fff]+', text)
        chinese_words = []
        for chars in chinese_chars:
            for i in range(len(chars)):
                for j in range(i+2, min(i+5, len(chars)+1)):
                    word = chars[i:j]
                    if word not in self.stop_words and len(word) >= 2:
                        chinese_words.append(word)

        # 提取英文词汇
        english_words = [
            word for word in re.findall(r'[a-zA-Z]+', text.lower())
            if word not in self.stop_words and len(word) >= 2
        ]

        all_words = chinese_words + english_words
        word_freq = Counter(all_words)

        # 获取top_n关键词
        top_keywords = word_freq.most_common(top_n)
        total_words = len(all_words) if all_words else 1

        keywords = [
            {
                "word": word,
                "frequency": freq,
                "percentage": round(freq / total_words * 100, 2)
            }
            for word, freq in top_keywords
        ]

        return keywords

    def analyze_sentiment(self, text: str) -> Dict[str, Any]:
        """
        情感分析

        Args:
            text: 输入文本

        Returns:
            情感分析结果字典
        """
        if not text:
            return {
                "sentiment": "neutral",
                "score": 0,
                "confidence": 0,
                "positive_words": [],
                "negative_words": [],
                "details": "文本为空"
            }

        # 分词（简单方法）
        chinese_words = []
        chinese_chars = re.findall(r'[\u4e00-\u9fff]+', text)
        for chars in chinese_chars:
            for i in range(len(chars)):
                for j in range(i+1, min(i+5, len(chars)+1)):
                    word = chars[i:j]
                    if len(word) >= 1:
                        chinese_words.append(word)

        english_words = re.findall(r'[a-zA-Z]+', text.lower())
        all_words = chinese_words + english_words

        # 情感分析
        positive_count = 0
        negative_count = 0
        positive_words_found = []
        negative_words_found = []
        sentiment_score = 0

        i = 0
        while i < len(all_words):
            word = all_words[i]
            multiplier = 1.0

            # 检查是否有程度副词
            if i > 0 and all_words[i-1] in self.intensifiers:
                multiplier = self.intensifiers[all_words[i-1]]

            # 检查是否有否定词
            if i > 0 and all_words[i-1] in self.negation_words:
                multiplier *= -1

            if word in self.positive_words:
                positive_count += 1
                positive_words_found.append(word)
                sentiment_score += 1 * multiplier
            elif word in self.negative_words:
                negative_count += 1
                negative_words_found.append(word)
                sentiment_score -= 1 * multiplier

            i += 1

        # 计算情感得分和置信度
        total_sentiment_words = positive_count + negative_count
        if total_sentiment_words == 0:
            sentiment = "neutral"
            confidence = 0
        else:
            if sentiment_score > 0.5:
                sentiment = "positive"
                confidence = min(abs(sentiment_score) / total_sentiment_words, 1.0)
            elif sentiment_score < -0.5:
                sentiment = "negative"
                confidence = min(abs(sentiment_score) / total_sentiment_words, 1.0)
            else:
                sentiment = "neutral"
                confidence = 0.5

        return {
            "sentiment": sentiment,
            "score": round(sentiment_score, 2),
            "confidence": round(confidence, 2),
            "positive_words": list(set(positive_words_found)),
            "negative_words": list(set(negative_words_found)),
            "positive_count": positive_count,
            "negative_count": negative_count,
            "details": f"发现{positive_count}个正面词，{negative_count}个负面词"
        }

    def analyze(self, text: str, options: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """
        执行完整的文本分析

        Args:
            text: 输入文本
            options: 分析选项，可包含：
                - keywords_top_n: 关键词数量（默认10）
                - enable_sentiment: 是否启用情感分析（默认True）
                - enable_keywords: 是否启用关键词提取（默认True）
                - enable_word_count: 是否启用词数统计（默认True）
                - enable_char_count: 是否启用字符统计（默认True）

        Returns:
            完整的分析结果字典
        """
        if options is None:
            options = {}

        if not isinstance(text, str):
            raise ValueError("输入文本必须是字符串类型")

        # 清洗文本：去除多余空白
        text = text.strip()
        if not text:
            return {
                "character_stats": self.count_characters(""),
                "word_stats": self.count_words(""),
                "keywords": [],
                "sentiment": self.analyze_sentiment(""),
                "text_length": 0
            }

        result = {
            "text_length": len(text)
        }

        # 字符统计
        if options.get("enable_char_count", True):
            result["character_stats"] = self.count_characters(text)

        # 词数统计
        if options.get("enable_word_count", True):
            result["word_stats"] = self.count_words(text)

        # 关键词提取
        if options.get("enable_keywords", True):
            top_n = options.get("keywords_top_n", 10)
            result["keywords"] = self.extract_keywords(text, top_n)

        # 情感分析
        if options.get("enable_sentiment", True):
            result["sentiment"] = self.analyze_sentiment(text)

        return result


def main(input_data: Dict[str, Any]) -> Dict[str, Any]:
    """
    主函数：文本分析器入口

    Args:
        input_data: 输入数据字典，必须包含：
            - text: 要分析的文本（必需）
            - options: 分析选项（可选）

    Returns:
        包含状态、消息和输出的JSON格式结果
    """
    try:
        # 参数验证
        if not isinstance(input_data, dict):
            return {
                "status": "error",
                "message": "输入数据必须是字典类型",
                "output": None
            }

        if "text" not in input_data:
            return {
                "status": "error",
                "message": "缺少必需参数 'text'",
                "output": None
            }

        text = input_data["text"]
        if not isinstance(text, str):
            return {
                "status": "error",
                "message": f"'text' 参数必须是字符串类型，当前类型: {type(text).__name__}",
                "output": None
            }

        # 文本长度检查
        if len(text) > 100000:
            return {
                "status": "error",
                "message": "文本长度超过限制（最大100000字符）",
                "output": None
            }

        options = input_data.get("options", {})

        # 创建分析器实例并执行分析
        logger.info(f"开始分析文本，长度: {len(text)} 字符")
        analyzer = TextAnalyzer()
        result = analyzer.analyze(text, options)

        logger.info("文本分析完成")
        return {
            "status": "success",
            "message": "文本分析成功完成",
            "output": result
        }

    except ValueError as e:
        logger.error(f"参数错误: {str(e)}")
        return {
            "status": "error",
            "message": f"参数错误: {str(e)}",
            "output": None
        }
    except Exception as e:
        logger.error(f"分析过程中发生错误: {str(e)}")
        return {
            "status": "error",
            "message": f"分析失败: {str(e)}",
            "output": None
        }


if __name__ == "__main__":
    """
    命令行运行示例：
    python text_analyzer.py '{"text": "今天天气真好，我非常开心！这是一个美好的日子。"}'
    """
    if len(sys.argv) > 1:
        try:
            # 从命令行参数读取JSON输入
            input_json = sys.argv[1]
            input_data = json.loads(input_json)
            result = main(input_data)
            print(json.dumps(result, ensure_ascii=False, indent=2))
        except json.JSONDecodeError as e:
            error_result = {
                "status": "error",
                "message": f"JSON解析错误: {str(e)}",
                "output": None
            }
            print(json.dumps(error_result, ensure_ascii=False, indent=2))
        except Exception as e:
            error_result = {
                "status": "error",
                "message": f"执行错误: {str(e)}",
                "output": None
            }
            print(json.dumps(error_result, ensure_ascii=False, indent=2))
    else:
        # 演示模式：使用示例文本
        sample_input = {
            "text": "今天天气真好，我非常开心！这是一个美好的日子。阳光明媚，空气清新，让人感到无比幸福。不过昨天天气很糟糕，下了大雨，让人心情低落。",
            "options": {
                "keywords_top_n": 5,
                "enable_sentiment": True,
                "enable_keywords": True,
                "enable_word_count": True,
                "enable_char_count": True
            }
        }
        print("=" * 60)
        print("文本分析器 - 演示模式")
        print("=" * 60)
        print(f"\n输入文本: {sample_input['text']}\n")
        print("分析选项:", json.dumps(sample_input['options'], ensure_ascii=False))
        print("\n" + "=" * 60)
        print("分析结果:")
        print("=" * 60)

        result = main(sample_input)
        print(json.dumps(result, ensure_ascii=False, indent=2))

        print("\n" + "=" * 60)
        print("使用方法: python text_analyzer.py '{\"text\": \"你的文本\"}'")
        print("=" * 60)