import 'package:flutter/foundation.dart' show kIsWeb;
import '../models/book.dart';
import 'database_service.dart';
import 'storage_service.dart';

class BookRepository {
  final List<Book> _books = [];
  final _databaseService = DatabaseService();

  List<Book> get books => List.unmodifiable(_books);

  Future<void> init() async {
    if (kIsWeb) {
      final loadedBooks = await StorageService.loadBooks();
      _books.clear();
      _books.addAll(loadedBooks);
    } else {
      final loadedBooks = await _databaseService.getAllBooks();
      _books.clear();
      _books.addAll(loadedBooks);
    }
  }

  Future<void> addBook(Book book) async {
    if (kIsWeb) {
      _books.add(book);
      await StorageService.saveBooks(_books);
    } else {
      await _databaseService.saveBook(book);
      _books.add(book);
    }
  }

  Future<void> updateBook(Book book) async {
    if (kIsWeb) {
      final index = _books.indexWhere((b) => b.id == book.id);
      if (index != -1) {
        _books[index] = book;
        await StorageService.saveBooks(_books);
      }
    } else {
      await _databaseService.updateBook(book);
      final index = _books.indexWhere((b) => b.id == book.id);
      if (index != -1) {
        _books[index] = book;
      }
    }
  }

  Future<void> deleteBook(String bookId) async {
    if (kIsWeb) {
      _books.removeWhere((book) => book.id == bookId);
      await StorageService.saveBooks(_books);
    } else {
      await _databaseService.deleteBook(bookId);
      _books.removeWhere((book) => book.id == bookId);
    }
  }
}
