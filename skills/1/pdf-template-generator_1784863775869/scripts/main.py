# -*- coding: utf-8 -*-
"""
PDF模板生成器 - 根据需求生成PDF文档模板

功能：
- 支持创建包含文本、表格、图片的PDF模板
- 支持自定义页面大小、边距、字体
- 支持页眉页脚
- 支持水印
- 支持多页文档生成

依赖：reportlab (标准库未包含，需安装: pip install reportlab)
"""

import json
import logging
import os
import sys
from datetime import datetime
from typing import Any, Dict, List, Optional, Union

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

try:
    from reportlab.lib.pagesizes import A4, LETTER, LEGAL, A3, A5
    from reportlab.lib.units import inch, mm, cm
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.lib.enums import TA_LEFT, TA_CENTER, TA_RIGHT, TA_JUSTIFY
    from reportlab.lib.colors import (
        black, white, red, blue, green, yellow, gray,
        HexColor, Color
    )
    from reportlab.platypus import (
        SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
        Image, PageBreak, KeepTogether, Frame, PageTemplate,
        BaseDocTemplate, NextPageTemplate, PageTemplate
    )
    from reportlab.platypus.flowables import HRFlowable
    from reportlab.platypus.tableofcontents import TableOfContents
    from reportlab.pdfgen import canvas
    from reportlab.pdfbase import pdfmetrics
    from reportlab.pdfbase.ttfonts import TTFont
    from reportlab.graphics.shapes import Drawing, Line, Rect, String
    from reportlab.graphics import renderPDF
    from reportlab.platypus.doctemplate import PageTemplate, BaseDocTemplate
    from reportlab.platypus.frames import Frame
    REPORTLAB_AVAILABLE = True
except ImportError:
    REPORTLAB_AVAILABLE = False
    logger.warning("reportlab 未安装，请执行: pip install reportlab")

# 页面大小映射
PAGE_SIZES = {
    "A4": A4,
    "A3": A3,
    "A5": A5,
    "LETTER": LETTER,
    "LEGAL": LEGAL,
}

# 对齐方式映射
ALIGNMENTS = {
    "left": TA_LEFT,
    "center": TA_CENTER,
    "right": TA_RIGHT,
    "justify": TA_JUSTIFY,
}

# 颜色映射
COLOR_MAP = {
    "black": black,
    "white": white,
    "red": red,
    "blue": blue,
    "green": green,
    "yellow": yellow,
    "gray": gray,
}


def _parse_color(color_value: Union[str, List[int], Dict[str, int]]) -> Any:
    """解析颜色值"""
    if isinstance(color_value, str):
        color_lower = color_value.lower()
        if color_lower in COLOR_MAP:
            return COLOR_MAP[color_lower]
        if color_lower.startswith("#"):
            return HexColor(color_lower)
        try:
            return HexColor(f"#{color_lower}")
        except Exception:
            return black
    elif isinstance(color_value, (list, tuple)) and len(color_value) == 3:
        return Color(
            color_value[0] / 255.0,
            color_value[1] / 255.0,
            color_value[2] / 255.0
        )
    elif isinstance(color_value, dict):
        r = color_value.get("r", 0)
        g = color_value.get("g", 0)
        b = color_value.get("b", 0)
        return Color(r / 255.0, g / 255.0, b / 255.0)
    return black


def _parse_page_size(page_size: Union[str, tuple, list]) -> tuple:
    """解析页面大小"""
    if isinstance(page_size, str):
        return PAGE_SIZES.get(page_size.upper(), A4)
    elif isinstance(page_size, (tuple, list)) and len(page_size) == 2:
        return tuple(page_size)
    return A4


class PDFTemplateGenerator:
    """PDF模板生成器核心类"""

    def __init__(self, config: Dict[str, Any]):
        """
        初始化PDF生成器

        Args:
            config: 配置字典，包含页面设置、内容等
        """
        self.config = config
        self.output_path = config.get("output_path", "output.pdf")
        self.page_size = _parse_page_size(config.get("page_size", "A4"))
        self.margins = config.get("margins", {
            "left": 72,
            "right": 72,
            "top": 72,
            "bottom": 72,
        })
        self.title = config.get("title", "PDF Document")
        self.author = config.get("author", "PDF Template Generator")
        self.subject = config.get("subject", "")
        self.keywords = config.get("keywords", "")
        self.watermark_text = config.get("watermark", "")
        self.watermark_color = _parse_color(config.get("watermark_color", "#CCCCCC"))
        self.watermark_opacity = config.get("watermark_opacity", 0.3)
        self.watermark_font_size = config.get("watermark_font_size", 60)
        self.watermark_angle = config.get("watermark_angle", 45)
        self.header_config = config.get("header", {})
        self.footer_config = config.get("footer", {})
        self.content = config.get("content", [])
        self.default_font = config.get("default_font", "Helvetica")
        self.default_font_size = config.get("default_font_size", 12)
        self.custom_fonts = config.get("custom_fonts", {})
        self.styles = getSampleStyleSheet()
        self._register_fonts()
        self._build_custom_styles()

    def _register_fonts(self):
        """注册自定义字体"""
        for font_name, font_path in self.custom_fonts.items():
            if os.path.exists(font_path):
                try:
                    pdfmetrics.registerFont(TTFont(font_name, font_path))
                    logger.info(f"已注册字体: {font_name} -> {font_path}")
                except Exception as e:
                    logger.warning(f"字体注册失败 {font_name}: {e}")

    def _build_custom_styles(self):
        """构建自定义样式"""
        custom_styles = self.config.get("styles", {})
        for style_name, style_config in custom_styles.items():
            try:
                parent_style = self.styles.get(
                    style_config.get("parent", "Normal"),
                    self.styles["Normal"]
                )
                new_style = ParagraphStyle(
                    name=style_name,
                    parent=parent_style,
                    fontName=style_config.get("fontName", self.default_font),
                    fontSize=style_config.get("fontSize", self.default_font_size),
                    leading=style_config.get("leading", None),
                    alignment=ALIGNMENTS.get(
                        style_config.get("alignment", "left"), TA_LEFT
                    ),
                    textColor=_parse_color(style_config.get("textColor", "black")),
                    spaceBefore=style_config.get("spaceBefore", 0),
                    spaceAfter=style_config.get("spaceAfter", 0),
                    leftIndent=style_config.get("leftIndent", 0),
                    rightIndent=style_config.get("rightIndent", 0),
                    firstLineIndent=style_config.get("firstLineIndent", 0),
                )
                self.styles.add(new_style)
            except Exception as e:
                logger.warning(f"样式构建失败 {style_name}: {e}")

    def _create_header_footer(self, canvas_obj, doc):
        """创建页眉和页脚"""
        canvas_obj.saveState()

        # 页眉
        if self.header_config.get("enabled", False):
            header_text = self.header_config.get("text", "")
            header_font = self.header_config.get("font", self.default_font)
            header_size = self.header_config.get("font_size", 10)
            header_color = _parse_color(self.header_config.get("color", "gray"))
            header_align = self.header_config.get("align", "center")

            canvas_obj.setFont(header_font, header_size)
            canvas_obj.setFillColor(header_color)

            page_width = self.page_size[0]
            top_margin = self.margins.get("top", 72)
            y_position = self.page_size[1] - top_margin + 20

            if header_align == "center":
                canvas_obj.drawCentredString(page_width / 2, y_position, header_text)
            elif header_align == "right":
                canvas_obj.drawRightString(
                    page_width - self.margins.get("right", 72), y_position, header_text
                )
            else:
                canvas_obj.drawString(
                    self.margins.get("left", 72), y_position, header_text
                )

            # 页眉分隔线
            if self.header_config.get("line", False):
                canvas_obj.setStrokeColor(header_color)
                canvas_obj.setLineWidth(0.5)
                line_y = self.page_size[1] - top_margin + 10
                canvas_obj.line(
                    self.margins.get("left", 72),
                    line_y,
                    page_width - self.margins.get("right", 72),
                    line_y,
                )

        # 页脚
        if self.footer_config.get("enabled", False):
            footer_text = self.footer_config.get("text", "")
            footer_font = self.footer_config.get("font", self.default_font)
            footer_size = self.footer_config.get("font_size", 10)
            footer_color = _parse_color(self.footer_config.get("color", "gray"))
            footer_align = self.footer_config.get("align", "center")

            canvas_obj.setFont(footer_font, footer_size)
            canvas_obj.setFillColor(footer_color)

            bottom_margin = self.margins.get("bottom", 72)
            y_position = bottom_margin - 20

            # 支持页码替换
            display_text = footer_text.replace("{page}", str(canvas_obj.getPageNumber()))

            if footer_align == "center":
                canvas_obj.drawCentredString(
                    self.page_size[0] / 2, y_position, display_text
                )
            elif footer_align == "right":
                canvas_obj.drawRightString(
                    self.page_size[0] - self.margins.get("right", 72),
                    y_position,
                    display_text,
                )
            else:
                canvas_obj.drawString(
                    self.margins.get("left", 72), y_position, display_text
                )

            # 页脚分隔线
            if self.footer_config.get("line", False):
                canvas_obj.setStrokeColor(footer_color)
                canvas_obj.setLineWidth(0.5)
                line_y = bottom_margin - 5
                canvas_obj.line(
                    self.margins.get("left", 72),
                    line_y,
                    self.page_size[0] - self.margins.get("right", 72),
                    line_y,
                )

        canvas_obj.restoreState()

    def _create_watermark(self, canvas_obj, doc):
        """创建水印"""
        if not self.watermark_text:
            return

        canvas_obj.saveState()
        canvas_obj.setFillColor(self.watermark_color)
        canvas_obj.setFont(self.default_font, self.watermark_font_size)

        # 设置透明度
        try:
            canvas_obj.setFillAlpha(self.watermark_opacity)
        except AttributeError:
            pass

        # 旋转并居中绘制水印
        canvas_obj.translate(self.page_size[0] / 2, self.page_size[1] / 2)
        canvas_obj.rotate(self.watermark_angle)
        canvas_obj.drawCentredString(0, 0, self.watermark_text)

        canvas_obj.restoreState()

    def _on_page(self, canvas_obj, doc):
        """页面回调函数"""
        self._create_watermark(canvas_obj, doc)
        self._create_header_footer(canvas_obj, doc)

    def _process_text_element(self, element: Dict[str, Any]) -> List:
        """处理文本元素"""
        flowables = []
        text = element.get("text", "")
        style_name = element.get("style", "Normal")
        style = self.styles.get(style_name, self.styles["Normal"])

        if element.get("keep_with_next", False):
            flowables.append(KeepTogether([Paragraph(text, style)]))
        else:
            flowables.append(Paragraph(text, style))

        # 添加后续间距
        spacing = element.get("spacing", 0)
        if spacing > 0:
            flowables.append(Spacer(1, spacing))

        return flowables

    def _process_heading_element(self, element: Dict[str, Any]) -> List:
        """处理标题元素"""
        flowables = []
        level = element.get("level", 1)
        text = element.get("text", "")

        heading_styles = {
            1: "Heading1",
            2: "Heading2",
            3: "Heading3",
            4: "Heading4",
        }
        style_name = element.get("style", heading_styles.get(level, "Heading1"))
        style = self.styles.get(style_name, self.styles["Heading1"])

        flowables.append(Paragraph(text, style))
        spacing = element.get("spacing", 12)
        if spacing > 0:
            flowables.append(Spacer(1, spacing))

        return flowables

    def _process_table_element(self, element: Dict[str, Any]) -> List:
        """处理表格元素"""
        flowables = []
        table_data = element.get("data", [])
        col_widths = element.get("col_widths", None)
        row_heights = element.get("row_heights", None)
        style_config = element.get("style", {})

        if not table_data:
            return flowables

        # 创建表格
        table = Table(table_data, colWidths=col_widths, rowHeights=row_heights)

        # 构建表格样式
        table_style_commands = []

        # 边框
        if style_config.get("grid", True):
            grid_color = _parse_color(style_config.get("grid_color", "black"))
            grid_width = style_config.get("grid_width", 0.5)
            table_style_commands.append(("GRID", (0, 0), (-1, -1), grid_width, grid_color))

        # 背景色
        bg_colors = style_config.get("background_colors", [])
        for bg_config in bg_colors:
            rows = bg_config.get("rows", [0])
            color = _parse_color(bg_config.get("color", "white"))
            for row in rows:
                table_style_commands.append(
                    ("BACKGROUND", (0, row), (-1, row), color)
                )

        # 字体设置
        font_name = style_config.get("font", self.default_font)
        font_size = style_config.get("font_size", 10)
        text_color = _parse_color(style_config.get("text_color", "black"))
        table_style_commands.append(
            ("FONTNAME", (0, 0), (-1, -1), font_name)
        )
        table_style_commands.append(
            ("FONTSIZE", (0, 0), (-1, -1), font_size)
        )
        table_style_commands.append(
            ("TEXTCOLOR", (0, 0), (-1, -1), text_color)
        )

        # 对齐方式
        alignment = ALIGNMENTS.get(style_config.get("alignment", "left"), TA_LEFT)
        table_style_commands.append(
            ("ALIGN", (0, 0), (-1, -1), alignment)
        )

        # 垂直对齐
        valign = style_config.get("valign", "MIDDLE")
        table_style_commands.append(
            ("VALIGN", (0, 0), (-1, -1), valign)
        )

        # 内边距
        padding = style_config.get("padding", 4)
        table_style_commands.append(
            ("TOPPADDING", (0, 0), (-1, -1), padding)
        )
        table_style_commands.append(
            ("BOTTOMPADDING", (0, 0), (-1, -1), padding)
        )
        table_style_commands.append(
            ("LEFTPADDING", (0, 0), (-1, -1), padding)
        )
        table_style_commands.append(
            ("RIGHTPADDING", (0, 0), (-1, -1), padding)
        )

        # 合并单元格
        merges = style_config.get("merges", [])
        for merge in merges:
            table_style_commands.append(
                ("SPAN", merge.get("start", (0, 0)), merge.get("end", (0, 0)))
            )

        table.setStyle(TableStyle(table_style_commands))
        flowables.append(table)

        spacing = element.get("spacing", 12)
        if spacing > 0:
            flowables.append(Spacer(1, spacing))

        return flowables

    def _process_image_element(self, element: Dict[str, Any]) -> List:
        """处理图片元素"""
        flowables = []
        image_path = element.get("path", "")
        width = element.get("width", None)
        height = element.get("height", None)
        alignment = element.get("alignment", "center")

        if not image_path or not os.path.exists(image_path):
            logger.warning(f"图片不存在: {image_path}")
            # 添加占位符
            placeholder = Paragraph(
                f'<i>[图片未找到: {os.path.basename(image_path)}]</i>',
                self.styles["Normal"]
            )
            flowables.append(placeholder)
            return flowables

        try:
            img = Image(image_path, width=width, height=height)

            # 水平对齐
            if alignment == "center":
                img.hAlign = "CENTER"
            elif alignment == "right":
                img.hAlign = "RIGHT"
            else:
                img.hAlign = "LEFT"

            flowables.append(img)
        except Exception as e:
            logger.error(f"加载图片失败: {e}")
            placeholder = Paragraph(
                f'<i>[图片加载失败: {os.path.basename(image_path)}]</i>',
                self.styles["Normal"]
            )
            flowables.append(placeholder)

        spacing = element.get("spacing", 12)
        if spacing > 0:
            flowables.append(Spacer(1, spacing))

        return flowables

    def _process_line_element(self, element: Dict[str, Any]) -> List:
        """处理水平线元素"""
        flowables = []
        width = element.get("width", "100%")
        thickness = element.get("thickness", 1)
        color = _parse_color(element.get("color", "black"))
        style = element.get("line_style", "solid")

        hr = HRFlowable(
            width=width,
            thickness=thickness,
            color=color,
            spaceBefore=element.get("space_before", 6),
            spaceAfter=element.get("space_after", 6),
        )
        flowables.append(hr)

        return flowables

    def _process_page_break_element(self, element: Dict[str, Any]) -> List:
        """处理分页元素"""
        return [PageBreak()]

    def _process_spacer_element(self, element: Dict[str, Any]) -> List:
        """处理间距元素"""
        height = element.get("height", 12)
        return [Spacer(1, height)]

    def _process_element(self, element: Dict[str, Any]) -> List:
        """处理单个元素"""
        element_type = element.get("type", "text").lower()

        processors = {
            "text": self._process_text_element,
            "paragraph": self._process_text_element,
            "heading": self._process_heading_element,
            "title": self._process_heading_element,
            "table": self._process_table_element,
            "image": self._process_image_element,
            "line": self._process_line_element,
            "hr": self._process_line_element,
            "page_break": self._process_page_break_element,
            "spacer": self._process_spacer_element,
            "space": self._process_spacer_element,
        }

        processor = processors.get(element_type)
        if processor:
            try:
                return processor(element)
            except Exception as e:
                logger.error(f"处理元素失败 [{element_type}]: {e}")
                return [Paragraph(
                    f'<font color="red">[元素处理错误: {element_type}]</font>',
                    self.styles["Normal"]
                )]
        else:
            logger.warning(f"未知元素类型: {element_type}")
            return [Paragraph(
                f'<i>[未知元素类型: {element_type}]</i>',
                self.styles["Normal"]
            )]

    def generate(self) -> str:
        """
        生成PDF文档

        Returns:
            生成的PDF文件路径
        """
        if not REPORTLAB_AVAILABLE:
            raise ImportError("reportlab 未安装，请执行: pip install reportlab")

        # 确保输出目录存在
        output_dir = os.path.dirname(os.path.abspath(self.output_path))
        if output_dir and not os.path.exists(output_dir):
            os.makedirs(output_dir, exist_ok=True)

        # 创建文档模板
        doc = SimpleDocTemplate(
            self.output_path,
            pagesize=self.page_size,
            leftMargin=self.margins.get("left", 72),
            rightMargin=self.margins.get("right", 72),
            topMargin=self.margins.get("top", 72),
            bottomMargin=self.margins.get("bottom", 72),
            title=self.title,
            author=self.author,
            subject=self.subject,
            keywords=self.keywords,
        )

        # 构建内容流
        story = []

        # 处理所有内容元素
        for element in self.content:
            story.extend(self._process_element(element))

        # 生成PDF
        try:
            doc.build(story, onFirstPage=self._on_page, onLaterPages=self._on_page)
            logger.info(f"PDF生成成功: {self.output_path}")
            return self.output_path
        except Exception as e:
            logger.error(f"PDF生成失败: {e}")
            raise


def main(input_data: Union[str, Dict[str, Any]]) -> Dict[str, Any]:
    """
    PDF模板生成器入口函数

    Args:
        input_data: JSON字符串或字典，包含PDF生成配置

    Returns:
        包含status, message, output的字典
    """
    try:
        # 解析输入数据
        if isinstance(input_data, str):
            try:
                config = json.loads(input_data)
            except json.JSONDecodeError as e:
                return {
                    "status": "error",
                    "message": f"JSON解析失败: {str(e)}",
                    "output": None,
                }
        elif isinstance(input_data, dict):
            config = input_data
        else:
            return {
                "status": "error",
                "message": f"输入数据类型错误，期望str或dict，实际为{type(input_data).__name__}",
                "output": None,
            }

        # 验证必要参数
        if not config.get("output_path"):
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            config["output_path"] = f"output_{timestamp}.pdf"
            logger.info(f"未指定输出路径，使用默认路径: {config['output_path']}")

        if not config.get("content"):
            return {
                "status": "error",
                "message": "缺少必要参数: content（PDF内容不能为空）",
                "output": None,
            }

        # 创建生成器并生成PDF
        generator = PDFTemplateGenerator(config)
        output_path = generator.generate()

        # 获取文件信息
        file_size = os.path.getsize(output_path)
        abs_path = os.path.abspath(output_path)

        result = {
            "status": "success",
            "message": f"PDF模板生成成功",
            "output": {
                "file_path": abs_path,
                "file_name": os.path.basename(output_path),
                "file_size": file_size,
                "file_size_human": _format_file_size(file_size),
                "page_size": config.get("page_size", "A4"),
                "elements_count": len(config.get("content", [])),
                "generated_at": datetime.now().isoformat(),
            },
        }

        logger.info(f"PDF生成完成: {abs_path} ({_format_file_size(file_size)})")
        return result

    except ImportError as e:
        return {
            "status": "error",
            "message": f"缺少依赖库: {str(e)}。请执行: pip install reportlab",
            "output": None,
        }
    except Exception as e:
        logger.error(f"PDF生成异常: {str(e)}", exc_info=True)
        return {
            "status": "error",
            "message": f"PDF生成失败: {str(e)}",
            "output": None,
        }


def _format_file_size(size_bytes: int) -> str:
    """格式化文件大小"""
    for unit in ["B", "KB", "MB", "GB"]:
        if size_bytes < 1024:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024
    return f"{size_bytes:.1f} TB"


# 示例配置模板
EXAMPLE_CONFIG = {
    "output_path": "example_template.pdf",
    "page_size": "A4",
    "title": "示例PDF模板",
    "author": "PDF模板生成器",
    "subject": "演示文档",
    "keywords": "pdf, template, example",
    "margins": {
        "left": 72,
        "right": 72,
        "top": 72,
        "bottom": 72,
    },
    "watermark": "机密文件",
    "watermark_color": "#CCCCCC",
    "watermark_opacity": 0.15,
    "watermark_angle": 45,
    "header": {
        "enabled": True,
        "text": "PDF模板生成器 - 示例文档",
        "font": "Helvetica",
        "font_size": 9,
        "color": "gray",
        "align": "center",
        "line": True,
    },
    "footer": {
        "enabled": True,
        "text": "第 {page} 页",
        "font": "Helvetica",
        "font_size": 9,
        "color": "gray",
        "align": "center",
        "line": True,
    },
    "content": [
        {
            "type": "heading",
            "level": 1,
            "text": "PDF模板生成器",
            "spacing": 24,
        },
        {
            "type": "text",
            "text": "这是一个使用Python生成的PDF模板示例文档。该生成器支持多种元素类型，包括文本、表格、图片、水平线等。",
            "style": "Normal",
            "spacing": 12,
        },
        {
            "type": "heading",
            "level": 2,
            "text": "功能特性",
            "spacing": 16,
        },
        {
            "type": "text",
            "text": "• 支持自定义页面大小（A3/A4/A5/LETTER/LEGAL）<br/>"
                   "• 支持页眉和页脚<br/>"
                   "• 支持水印功能<br/>"
                   "• 支持表格生成<br/>"
                   "• 支持图片插入<br/>"
                   "• 支持自定义样式",
            "style": "Normal",
            "spacing": 12,
        },
        {
            "type": "heading",
            "level": 2,
            "text": "示例表格",
            "spacing": 16,
        },
        {
            "type": "table",
            "data": [
                ["序号", "项目名称", "数量", "单价", "金额"],
                ["1", "商品A", "10", "100.00", "1,000.00"],
                ["2", "商品B", "5", "200.00", "1,000.00"],
                ["3", "商品C", "20", "50.00", "1,000.00"],
                ["", "", "", "合计", "3,000.00"],
            ],
            "style": {
                "grid": True,
                "grid_color": "#333333",
                "grid_width": 0.5,
                "background_colors": [
                    {"rows": [0], "color": "#4472C4"},
                    {"rows": [-1], "color": "#D9E2F3"},
                ],
                "font": "Helvetica-Bold",
                "font_size": 10,
                "text_color": "black",
                "alignment": "center",
                "padding": 6,
                "merges": [
                    {"start": (0, -1), "end": (2, -1)},
                ],
            },
            "spacing": 24,
        },
        {
            "type": "text",
            "text": "<b>说明：</b>以上表格展示了基本的表格功能，包括单元格合并、背景色设置、边框样式等。",
            "style": "Normal",
            "spacing": 12,
        },
    ],
}


if __name__ == "__main__":
    # 命令行直接运行
    if len(sys.argv) > 1:
        # 从命令行参数读取JSON配置
        input_json = sys.argv[1]
        result = main(input_json)
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        # 使用示例配置生成演示PDF
        print("=" * 60)
        print("PDF模板生成器 - 演示模式")
        print("=" * 60)
        print("\n使用示例配置生成演示PDF...")
        print(f"输出文件: {EXAMPLE_CONFIG['output_path']}")

        result = main(EXAMPLE_CONFIG)

        if result["status"] == "success":
            print(f"\n✓ PDF生成成功!")
            print(f"  文件路径: {result['output']['file_path']}")
            print(f"  文件大小: {result['output']['file_size_human']}")
            print(f"  元素数量: {result['output']['elements_count']}")
        else:
            print(f"\n✗ PDF生成失败: {result['message']}")

        print("\n" + "=" * 60)
        print("使用方法:")
        print("  python pdf_template_generator.py '<json_config>'")
        print("  或直接运行以查看演示")
        print("=" * 60)