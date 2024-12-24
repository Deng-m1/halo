import 'dart:core';
import 'package:my_app/models/chapter.dart';

class Book {
  final String id; // 书籍唯一标识
  final String title; // 书籍标题
  final String author; // 作者
  final String coverPath; // 封面图片路径
  final int lastReadChapter; // 最后阅读的章节
  final int lastReadPosition; // 最后阅读的位置
  final DateTime lastReadTime; // 最后阅读时间
  final List<Chapter> chapters; // 章节列表

  Book({
    required this.id,
    required this.title,
    required this.author,
    required this.coverPath,
    this.lastReadChapter = 0,
    this.lastReadPosition = 0,
    DateTime? lastReadTime,
    this.chapters = const [],
  }) : lastReadTime = lastReadTime ?? DateTime.now();

  // 从JSON创建Book对象
  factory Book.fromJson(Map<String, dynamic> json) {
    return Book(
      id: json['id'] as String,
      title: json['title'] as String,
      author: json['author'] as String,
      coverPath: json['coverPath'] as String,
      lastReadChapter: json['lastReadChapter'] as int? ?? 0,
      lastReadPosition: json['lastReadPosition'] as int? ?? 0,
      lastReadTime: json['lastReadTime'] != null
          ? DateTime.parse(json['lastReadTime'] as String)
          : null,
      chapters: (json['chapters'] as List<dynamic>?)
              ?.map((e) => Chapter.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }

  // 转换为JSON
  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'author': author,
      'coverPath': coverPath,
      'lastReadChapter': lastReadChapter,
      'lastReadPosition': lastReadPosition,
      'lastReadTime': lastReadTime.toIso8601String(),
      'chapters': chapters.map((chapter) => chapter.toJson()).toList(),
    };
  }

  // 创建Book的副本
  Book copyWith({
    String? id,
    String? title,
    String? author,
    String? coverPath,
    int? lastReadChapter,
    int? lastReadPosition,
    DateTime? lastReadTime,
    List<Chapter>? chapters,
  }) {
    return Book(
      id: id ?? this.id,
      title: title ?? this.title,
      author: author ?? this.author,
      coverPath: coverPath ?? this.coverPath,
      lastReadChapter: lastReadChapter ?? this.lastReadChapter,
      lastReadPosition: lastReadPosition ?? this.lastReadPosition,
      lastReadTime: lastReadTime ?? this.lastReadTime,
      chapters: chapters ?? this.chapters,
    );
  }
}
