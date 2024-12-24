import 'package:sqflite_common_ffi/sqflite_ffi.dart';
import 'package:path/path.dart';
import '../models/book.dart';
import '../models/chapter.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

class DatabaseService {
  static final DatabaseService _instance = DatabaseService._internal();
  static Database? _database;
  static bool _initialized = false;

  factory DatabaseService() => _instance;
  DatabaseService._internal();

  static Future<void> initialize() async {
    if (kIsWeb) return; // Web平台不初始化SQLite
    if (!_initialized) {
      sqfliteFfiInit();
      databaseFactory = databaseFactoryFfi;
      _initialized = true;
    }
  }

  Future<Database> get database async {
    if (kIsWeb) {
      throw UnsupportedError('Web平台不支持SQLite数据库');
    }
    if (!_initialized) {
      await initialize();
    }
    _database ??= await _initDatabase();
    return _database!;
  }

  Future<Database> _initDatabase() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'books.db');

    return await openDatabase(
      path,
      version: 1,
      onCreate: _createTables,
    );
  }

  Future<void> _createTables(Database db, int version) async {
    // 书籍表
    await db.execute('''
      CREATE TABLE books (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        author TEXT NOT NULL,
        cover_path TEXT NOT NULL,
        last_read_chapter INTEGER NOT NULL DEFAULT 0,
        last_read_position INTEGER NOT NULL DEFAULT 0,
        last_read_time TEXT NOT NULL
      )
    ''');

    // 章节表
    await db.execute('''
      CREATE TABLE chapters (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        book_id TEXT NOT NULL,
        title TEXT NOT NULL,
        index_num INTEGER NOT NULL,
        start_position INTEGER NOT NULL,
        end_position INTEGER NOT NULL,
        FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
      )
    ''');

    // 文件内容表
    await db.execute('''
      CREATE TABLE book_contents (
        book_id TEXT PRIMARY KEY,
        content TEXT NOT NULL,
        FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
      )
    ''');
  }

  // 保存书籍
  Future<void> saveBook(Book book) async {
    final db = await database;
    await db.transaction((txn) async {
      // 保存书籍信息
      await txn.insert(
        'books',
        {
          'id': book.id,
          'title': book.title,
          'author': book.author,
          'cover_path': book.coverPath,
          'last_read_chapter': book.lastReadChapter,
          'last_read_position': book.lastReadPosition,
          'last_read_time': book.lastReadTime.toIso8601String(),
        },
        conflictAlgorithm: ConflictAlgorithm.replace,
      );

      // 保存章节信息
      for (var chapter in book.chapters) {
        await txn.insert(
          'chapters',
          {
            'book_id': book.id,
            'title': chapter.title,
            'index_num': chapter.index,
            'start_position': chapter.startPosition,
            'end_position': chapter.endPosition,
          },
          conflictAlgorithm: ConflictAlgorithm.replace,
        );
      }
    });
  }

  // 获取所有书籍
  Future<List<Book>> getAllBooks() async {
    final db = await database;
    final List<Map<String, dynamic>> bookMaps = await db.query('books');

    return Future.wait(bookMaps.map((bookMap) async {
      final List<Map<String, dynamic>> chapterMaps = await db.query(
        'chapters',
        where: 'book_id = ?',
        whereArgs: [bookMap['id']],
        orderBy: 'index_num ASC',
      );

      final chapters = chapterMaps
          .map((chapterMap) => Chapter(
                index: chapterMap['index_num'] as int,
                title: chapterMap['title'] as String,
                startPosition: chapterMap['start_position'] as int,
                endPosition: chapterMap['end_position'] as int,
              ))
          .toList();

      return Book(
        id: bookMap['id'] as String,
        title: bookMap['title'] as String,
        author: bookMap['author'] as String,
        coverPath: bookMap['cover_path'] as String,
        lastReadChapter: bookMap['last_read_chapter'] as int,
        lastReadPosition: bookMap['last_read_position'] as int,
        lastReadTime: DateTime.parse(bookMap['last_read_time'] as String),
        chapters: chapters,
      );
    }));
  }

  // 更新书籍
  Future<void> updateBook(Book book) async {
    await saveBook(book); // 使用REPLACE策略
  }

  // 删除书籍
  Future<void> deleteBook(String bookId) async {
    final db = await database;
    await db.delete(
      'books',
      where: 'id = ?',
      whereArgs: [bookId],
    );
  }
}
