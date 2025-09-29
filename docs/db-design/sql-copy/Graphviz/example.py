# -*- coding: utf-8 -*-
from graphviz import Digraph

# 创建一个有向图（ER图通常用无向图，但这里为了演示，也可使用有向图并合理设置）
# 设置中文字体支持，使用微软雅黑字体
er = Digraph('ER图', format='png', encoding='utf-8')

# 设置图形的一些属性，使看起来更美观
er.attr(rankdir='LR', size='8,5', fontname='Microsoft YaHei')

# 定义实体
er.node('学生', shape='box', fontname='Microsoft YaHei')
er.node('课程', shape='box', fontname='Microsoft YaHei')
er.node('教师', shape='box', fontname='Microsoft YaHei')

# 定义属性
er.node('学号', shape='ellipse', fontname='Microsoft YaHei')
er.node('学生姓名', shape='ellipse', fontname='Microsoft YaHei')
er.node('课程号', shape='ellipse', fontname='Microsoft YaHei')
er.node('课程名', shape='ellipse', fontname='Microsoft YaHei')
er.node('教师编号', shape='ellipse', fontname='Microsoft YaHei')
er.node('教师姓名', shape='ellipse', fontname='Microsoft YaHei')
er.node('成绩', shape='ellipse', fontname='Microsoft YaHei')

# 建立实体与属性的连接
er.edge('学号', '学生', fontname='Microsoft YaHei')
er.edge('学生姓名', '学生', fontname='Microsoft YaHei')
er.edge('课程号', '课程', fontname='Microsoft YaHei')
er.edge('课程名', '课程', fontname='Microsoft YaHei')
er.edge('教师编号', '教师', fontname='Microsoft YaHei')
er.edge('教师姓名', '教师', fontname='Microsoft YaHei')

# 定义关系（这里用菱形表示关系）
er.node('选修', shape='diamond', fontname='Microsoft YaHei')
er.node('讲授', shape='diamond', fontname='Microsoft YaHei')

# 建立实体与关系的连接，并标注 cardinality（m、n、1等）
er.edge('学生', '选修', label='m', fontname='Microsoft YaHei')
er.edge('选修', '课程', label='n', fontname='Microsoft YaHei')
er.edge('选修', '成绩', fontname='Microsoft YaHei')
er.edge('课程', '讲授', label='n', fontname='Microsoft YaHei')
er.edge('讲授', '教师', label='1', fontname='Microsoft YaHei')

# 渲染并保存图形
er.render('er_diagram', view=True)