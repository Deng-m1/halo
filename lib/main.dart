import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'pages/reader_page.dart';
import 'models/book.dart';
import 'services/book_service.dart';
import 'dart:io';
import 'services/book_repository.dart';
import 'services/database_service.dart';
import 'services/storage_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  if (kIsWeb) {
    await StorageService.initialize();
  } else {
    await DatabaseService.initialize();
    try {
      await getApplicationDocumentsDirectory();
    } catch (e) {
      debugPrint('初始化存储目录失败: $e');
    }
  }

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '我的阅读器',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const BookshelfPage(),
    );
  }
}

// 书架页面
class BookshelfPage extends StatefulWidget {
  const BookshelfPage({super.key});

  @override
  State<BookshelfPage> createState() => _BookshelfPageState();
}

class _BookshelfPageState extends State<BookshelfPage> {
  final _bookRepository = BookRepository();

  @override
  void initState() {
    super.initState();
    _loadBooks();
  }

  Future<void> _loadBooks() async {
    try {
      await _bookRepository.init();
      setState(() {});
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载书籍失败：${e.toString()}')),
        );
      }
    }
  }

  Future<void> _importBook() async {
    try {
      final book = await BookService.importBook();
      if (book != null) {
        await _bookRepository.addBook(book);
        setState(() {});
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('导入成功')),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('导入失败：${e.toString()}')),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final books = _bookRepository.books;
    return Scaffold(
      appBar: AppBar(
        title: const Text('我的书架'),
        actions: [
          // 添加更多按钮
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) async {
              switch (value) {
                case 'import':
                  await _importBook();
                  break;
                case 'settings':
                  // TODO: 实现设置功能
                  break;
              }
            },
            itemBuilder: (BuildContext context) => <PopupMenuEntry<String>>[
              const PopupMenuItem<String>(
                value: 'import',
                child: Row(
                  children: [
                    Icon(Icons.file_upload, size: 20),
                    SizedBox(width: 8),
                    Text('导入书籍'),
                  ],
                ),
              ),
              const PopupMenuItem<String>(
                value: 'settings',
                child: Row(
                  children: [
                    Icon(Icons.settings, size: 20),
                    SizedBox(width: 8),
                    Text('设置'),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
      body: books.isEmpty
          ? _buildEmptyView()
          : GridView.builder(
              padding: const EdgeInsets.all(16),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 3,
                childAspectRatio: 0.7,
                crossAxisSpacing: 16,
                mainAxisSpacing: 16,
              ),
              itemCount: books.length,
              itemBuilder: (context, index) {
                return _buildBookItem(books[index]);
              },
            ),
      floatingActionButton: FloatingActionButton(
        onPressed: _importBook,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildEmptyView() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.library_books,
            size: 64,
            color: Colors.grey[400],
          ),
          const SizedBox(height: 16),
          Text(
            '书架空空如也',
            style: TextStyle(
              fontSize: 16,
              color: Colors.grey[600],
            ),
          ),
          const SizedBox(height: 8),
          TextButton.icon(
            onPressed: _importBook,
            icon: const Icon(Icons.add),
            label: const Text('导入书籍'),
          ),
        ],
      ),
    );
  }

  Widget _buildBookItem(Book book) {
    return GestureDetector(
      onTap: () => _openReader(book),
      child: Column(
        children: [
          Expanded(
            child: Container(
              decoration: BoxDecoration(
                color: Colors.grey[200],
                borderRadius: BorderRadius.circular(8),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withOpacity(0.1),
                    blurRadius: 4,
                    offset: const Offset(0, 2),
                  ),
                ],
              ),
              child: Stack(
                children: [
                  if (book.coverPath.isNotEmpty)
                    ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: Image.file(
                        File(book.coverPath),
                        fit: BoxFit.cover,
                        width: double.infinity,
                        height: double.infinity,
                        errorBuilder: (context, error, stackTrace) {
                          return _buildDefaultCover(book);
                        },
                      ),
                    )
                  else
                    _buildDefaultCover(book),
                ],
              ),
            ),
          ),
          const SizedBox(height: 8),
          Text(
            book.title,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 14),
          ),
        ],
      ),
    );
  }

  Widget _buildDefaultCover(Book book) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.primaries[book.title.hashCode % Colors.primaries.length],
        borderRadius: BorderRadius.circular(8),
      ),
      child: Center(
        child: Text(
          book.title.isNotEmpty ? book.title.substring(0, 1) : '?',
          style: const TextStyle(
            color: Colors.white,
            fontSize: 32,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }

  void _openReader(Book book) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => ReaderPage(
          book: book,
          initialChapter: book.lastReadChapter,
        ),
      ),
    );
  }
}
