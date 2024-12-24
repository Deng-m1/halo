import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/book.dart';

class StorageService {
  static const String _booksKey = 'books';
  static SharedPreferences? _prefs;

  static Future<void> initialize() async {
    _prefs = await SharedPreferences.getInstance();
  }

  // 保存所有书籍信息
  static Future<void> saveBooks(List<Book> books) async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    final booksJson = books.map((book) => book.toJson()).toList();
    await prefs.setString(_booksKey, jsonEncode(booksJson));
  }

  // 读取所有书籍信息
  static Future<List<Book>> loadBooks() async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    final String? booksJsonString = prefs.getString(_booksKey);

    if (booksJsonString == null || booksJsonString.isEmpty) {
      return [];
    }

    try {
      final List<dynamic> booksJson = jsonDecode(booksJsonString);
      return booksJson
          .map((json) => Book.fromJson(json as Map<String, dynamic>))
          .toList();
    } catch (e) {
      print('读取书籍信息失败: $e');
      return [];
    }
  }

  // 保存书籍内容
  static Future<void> saveBookContent(String bookId, String content) async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    await prefs.setString('book_content_$bookId', content);
  }

  // 读取书籍内容
  static Future<String?> loadBookContent(String bookId) async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    return prefs.getString('book_content_$bookId');
  }

  // 删除书籍内容
  static Future<void> deleteBookContent(String bookId) async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    await prefs.remove('book_content_$bookId');
  }

  // 清除所有数据
  static Future<void> clearAll() async {
    final prefs = _prefs ?? await SharedPreferences.getInstance();
    await prefs.clear();
  }
}
