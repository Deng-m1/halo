import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import '../models/book.dart';
import '../models/chapter.dart';
import 'package:crypto/crypto.dart';
import 'package:sqflite/sqflite.dart';
import 'dart:convert';
import 'database_service.dart';
import 'storage_service.dart';
import 'package:charset/charset.dart';

class BookService {
  static final _db = DatabaseService();

  static Future<Book?> importBook() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['txt'],
        withData: true,
      );

      if (result == null) return null;

      String content;
      final bytes = result.files.first.bytes!;

      try {
        content = utf8.decode(bytes);
      } catch (e) {
        try {
          content = const GbkCodec(allowMalformed: true).decode(bytes);
        } catch (e) {
          content = latin1.decode(bytes);
        }
      }

      final id = sha1.convert(utf8.encode(content)).toString();
      final chapters = _parseChapters(content);
      final book = Book(
        id: id,
        title: result.files.first.name.replaceAll('.txt', ''),
        author: '未知',
        coverPath: '',
        chapters: chapters,
      );

      if (kIsWeb) {
        await StorageService.saveBookContent(id, content);
      } else {
        final db = await _db.database;
        await db.insert(
          'book_contents',
          {'book_id': id, 'content': content},
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }

      return book;
    } catch (e) {
      rethrow;
    }
  }

  static List<Chapter> _parseChapters(String content) {
    final chapters = <Chapter>[];
    final chapterPatterns = [
      // 中文数字章节
      RegExp(r'第[零一二三四五六七八九十百千万]+章[^\n]*'),
      RegExp(r'第[零一二三四五六七八九十百千万]+回[^\n]*'),
      RegExp(r'第[零一二三四五六七八九十百千万]+节[^\n]*'),

      // 阿拉伯数字章节
      RegExp(r'第\d+章[^\n]*'),
      RegExp(r'第\d+回[^\n]*'),
      RegExp(r'第\d+节[^\n]*'),

      // 带小数点的章节
      RegExp(r'^\d+\.\d+[^\n]*'),
      RegExp(r'^\d+\.[^\n]*'),

      // 特殊格式
      RegExp(r'Chapter\s*\d+[^\n]*', caseSensitive: false),
      RegExp(r'第[零一二三四五六七八九十百千万\d]+卷[^\n]*'),
      RegExp(r'序章[^\n]*'),
      RegExp(r'终章[^\n]*'),
      RegExp(r'尾声[^\n]*'),
      RegExp(r'后记[^\n]*'),
    ];

    int index = 0;

    for (int i = 0; i < content.length; i++) {
      for (var pattern in chapterPatterns) {
        // 计算可用的最大长度
        final remainingLength = content.length - i;
        final maxLength = remainingLength < 100 ? remainingLength : 100;

        if (maxLength < 10) continue; // 如果剩余文本太短就跳过

        final potentialTitle = content.substring(i, i + maxLength);
        final match = pattern.matchAsPrefix(potentialTitle);

        if (match != null) {
          if (index > 0) {
            chapters[index - 1] = chapters[index - 1].copyWith(
              endPosition: i,
            );
          }

          chapters.add(Chapter(
            index: index,
            title: match.group(0)!.trim(),
            startPosition: i,
            endPosition: content.length,
          ));

          index++;
          i += match.group(0)!.length - 1; // 减1是因为外层循环会加1
          break; // 找到一个匹配就跳出内层循环
        }
      }
    }

    // 如果没有检测到章节，创建单章节
    if (chapters.isEmpty) {
      chapters.add(Chapter(
        index: 0,
        title: '全文',
        startPosition: 0,
        endPosition: content.length,
      ));
    }

    return chapters;
  }

  static Future<String> readChapterContent(
      String bookId, Chapter chapter) async {
    if (kIsWeb) {
      final content = await StorageService.loadBookContent(bookId);
      return content ?? '内容加载失败';
    } else {
      final db = await _db.database;
      final result = await db.query(
        'book_contents',
        columns: ['content'],
        where: 'book_id = ?',
        whereArgs: [bookId],
      );
      return result.isNotEmpty ? result.first['content'] as String : '内容加载失败';
    }
  }
}
