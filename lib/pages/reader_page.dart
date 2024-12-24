import 'package:flutter/material.dart';
import '../models/book.dart';
import '../models/chapter.dart';
import '../services/book_service.dart';
import '../services/book_repository.dart';

class ReaderPage extends StatefulWidget {
  final Book book;
  final int initialChapter;

  const ReaderPage({
    super.key,
    required this.book,
    this.initialChapter = 0,
  });

  @override
  State<ReaderPage> createState() => _ReaderPageState();
}

class _ReaderPageState extends State<ReaderPage> {
  late Book _currentBook;
  late Chapter _currentChapter;
  late PageController _pageController;
  String _content = '';
  final List<String> _pageContents = [];
  int _currentPage = 0;
  bool _showBars = true;
  final _bookRepository = BookRepository();

  // 阅读设置
  double _fontSize = 18.0;
  final double _lineHeight = 1.5;
  Color _textColor = Colors.black87;
  Color _backgroundColor = Colors.white;
  final EdgeInsets _padding = const EdgeInsets.all(16.0);

  @override
  void initState() {
    super.initState();
    _currentBook = widget.book;
    _currentChapter = _currentBook.chapters[widget.initialChapter];
    _pageController = PageController();
    _pageController.addListener(_handlePageChanged);
    _loadChapterContent();
  }

  Future<void> _loadChapterContent() async {
    try {
      final content = await BookService.readChapterContent(
        _currentBook.id,
        _currentChapter,
      );
      if (mounted) {
        setState(() {
          _content = content;
          _splitContentIntoPages();
          _updateReadingProgress();
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('加载章节失败：${e.toString()}')),
        );
      }
    }
  }

  void _splitContentIntoPages() {
    _pageContents.clear();
    if (_content.isEmpty) return;

    // 移除多余的空白行和空格
    _content = _content
        .replaceAll(RegExp(r'\n{3,}'), '\n\n')
        .replaceAll(RegExp(r' {2,}'), ' ')
        .trim();

    final textSpan = TextSpan(
      text: _content,
      style: TextStyle(
        fontSize: _fontSize,
        height: _lineHeight,
        color: _textColor,
      ),
    );

    final textPainter = TextPainter(
      text: textSpan,
      textDirection: TextDirection.ltr,
      maxLines: null,
    );

    final maxWidth = MediaQuery.of(context).size.width - _padding.horizontal;
    final maxHeight = MediaQuery.of(context).size.height -
        MediaQuery.of(context).padding.vertical -
        _padding.vertical;

    int startOffset = 0;
    while (startOffset < _content.length) {
      textPainter.text = TextSpan(
        text: _content.substring(startOffset),
        style: TextStyle(
          fontSize: _fontSize,
          height: _lineHeight,
          color: _textColor,
        ),
      );

      textPainter.layout(maxWidth: maxWidth);

      final endPosition = textPainter.getPositionForOffset(
        Offset(maxWidth, maxHeight),
      );

      final endOffset = endPosition.offset;

      if (endOffset <= startOffset) break;

      String pageContent = _content.substring(startOffset, endOffset).trim();

      // 确保不会在单词中间断开
      if (endOffset < _content.length) {
        final nextSpace = _content.indexOf(' ', endOffset);
        final nextNewline = _content.indexOf('\n', endOffset);
        int nextBreak;

        if (nextSpace == -1 && nextNewline == -1) {
          nextBreak = _content.length;
        } else if (nextSpace == -1) {
          nextBreak = nextNewline;
        } else if (nextNewline == -1) {
          nextBreak = nextSpace;
        } else {
          nextBreak = nextSpace < nextNewline ? nextSpace : nextNewline;
        }

        if (nextBreak - endOffset < 10) {
          // 如果断点很近，就包含进当前页
          pageContent = _content.substring(startOffset, nextBreak).trim();
          startOffset = nextBreak;
        } else {
          startOffset = endOffset;
        }
      } else {
        startOffset = endOffset;
      }

      if (pageContent.isNotEmpty) {
        _pageContents.add(pageContent);
      }
    }

    if (_pageContents.isEmpty && _content.isNotEmpty) {
      _pageContents.add(_content);
    }
  }

  Future<void> _previousChapter() async {
    if (_currentChapter.index > 0) {
      final newChapter = _currentBook.chapters[_currentChapter.index - 1];
      setState(() {
        _currentChapter = newChapter;
        _content = '';
        _pageContents.clear();
        _currentPage = 0;
      });

      _pageController.removeListener(_handlePageChanged);
      _pageController.dispose();
      _pageController = PageController();
      _pageController.addListener(_handlePageChanged);

      await _loadChapterContent();
    }
  }

  Future<void> _nextChapter() async {
    if (_currentChapter.index < _currentBook.chapters.length - 1) {
      final newChapter = _currentBook.chapters[_currentChapter.index + 1];
      setState(() {
        _currentChapter = newChapter;
        _content = '';
        _pageContents.clear();
        _currentPage = 0;
      });

      _pageController.removeListener(_handlePageChanged);
      _pageController.dispose();
      _pageController = PageController();
      _pageController.addListener(_handlePageChanged);

      await _loadChapterContent();
    }
  }

  void _handlePageChanged() {
    if (_pageController.hasClients) {
      final page = _pageController.page?.round() ?? 0;
      if (page != _currentPage) {
        setState(() {
          _currentPage = page;
          _updateReadingProgress();
        });
      }
    }
  }

  void _handleTap(TapUpDetails details) {
    final screenWidth = MediaQuery.of(context).size.width;
    final tapX = details.globalPosition.dx;

    if (tapX < screenWidth / 3) {
      // 左侧区域：上一页或上一章
      if (_currentPage > 0) {
        _pageController.previousPage(
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeInOut,
        );
      } else {
        _previousChapter();
      }
    } else if (tapX > screenWidth * 2 / 3) {
      // 右侧区域：下一页或下一章
      if (_currentPage < _pageContents.length - 1) {
        _pageController.nextPage(
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeInOut,
        );
      } else {
        _nextChapter();
      }
    } else {
      setState(() {
        _showBars = !_showBars;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        children: [
          if (_pageContents.isEmpty)
            const Center(child: CircularProgressIndicator())
          else
            PageView.builder(
              controller: _pageController,
              onPageChanged: (index) {
                setState(() {
                  _currentPage = index;
                });
              },
              itemCount: _pageContents.length,
              itemBuilder: (context, index) {
                return GestureDetector(
                  onTapUp: _handleTap,
                  child: Container(
                    color: _backgroundColor,
                    padding: _padding,
                    child: Text(
                      _pageContents[index],
                      style: TextStyle(
                        fontSize: _fontSize,
                        height: _lineHeight,
                        color: _textColor,
                      ),
                    ),
                  ),
                );
              },
            ),
          // 顶部工具栏
          if (_showBars)
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: Container(
                color: Colors.black.withOpacity(0.8),
                child: SafeArea(
                  bottom: false,
                  child: Container(
                    height: 56,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    child: Row(
                      children: [
                        IconButton(
                          icon:
                              const Icon(Icons.arrow_back, color: Colors.white),
                          onPressed: () => Navigator.pop(context),
                        ),
                        Expanded(
                          child: Text(
                            _currentBook.title,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 18,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        PopupMenuButton<String>(
                          icon: const Icon(Icons.settings, color: Colors.white),
                          onSelected: _handleSettingSelection,
                          itemBuilder: (BuildContext context) => [
                            const PopupMenuItem<String>(
                              value: 'theme',
                              child: Text('主题设置'),
                            ),
                            const PopupMenuItem<String>(
                              value: 'font',
                              child: Text('字体设置'),
                            ),
                            const PopupMenuItem<String>(
                              value: 'chapters',
                              child: Text('目录'),
                            ),
                            const PopupMenuItem<String>(
                              value: 'info',
                              child: Text('书籍信息'),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          // 底部工具栏
          if (_showBars)
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Container(
                color: Colors.black.withOpacity(0.8),
                padding: EdgeInsets.only(
                  bottom: MediaQuery.of(context).padding.bottom,
                ),
                child: Container(
                  height: 56,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      TextButton(
                        onPressed: _previousChapter,
                        child: const Text(
                          '上一章',
                          style: TextStyle(color: Colors.white),
                        ),
                      ),
                      Text(
                        '${_currentPage + 1}/${_pageContents.length}页',
                        style: const TextStyle(color: Colors.white),
                      ),
                      TextButton(
                        onPressed: _nextChapter,
                        child: const Text(
                          '下一章',
                          style: TextStyle(color: Colors.white),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }

  Future<void> _updateReadingProgress() async {
    final updatedBook = _currentBook.copyWith(
      lastReadChapter: _currentChapter.index,
      lastReadPosition: _currentPage,
      lastReadTime: DateTime.now(),
    );
    await _bookRepository.updateBook(updatedBook);
    _currentBook = updatedBook;
  }

  void _handleSettingSelection(String value) {
    switch (value) {
      case 'theme':
        _toggleTheme();
        break;
      case 'font':
        _showFontSettings();
        break;
      case 'chapters':
        _showChapterList();
        break;
      case 'info':
        _showBookInfo();
        break;
    }
  }

  void _toggleTheme() {
    setState(() {
      if (_backgroundColor == Colors.white) {
        _backgroundColor = const Color(0xFF303030);
        _textColor = Colors.white70;
      } else {
        _backgroundColor = Colors.white;
        _textColor = Colors.black87;
      }
    });
  }

  void _showFontSettings() {
    showModalBottomSheet(
      context: context,
      builder: (context) => _buildReaderMenu(),
    );
  }

  void _showBookInfo() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('书籍信息'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('书名：${_currentBook.title}'),
            Text('作者：${_currentBook.author}'),
            Text('章节数：${_currentBook.chapters.length}'),
            Text('最后阅读：${_currentBook.lastReadTime.toString().split('.')[0]}'),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('关闭'),
          ),
        ],
      ),
    );
  }

  Widget _buildReaderMenu() {
    return Container(
      padding: const EdgeInsets.all(16),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          // 字体大小调节
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('字体大小'),
              Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.remove),
                    onPressed: () => _changeFontSize(-1),
                  ),
                  Text('${_fontSize.round()}'),
                  IconButton(
                    icon: const Icon(Icons.add),
                    onPressed: () => _changeFontSize(1),
                  ),
                ],
              ),
            ],
          ),
          // 章节选择
          ListTile(
            title: const Text('章节目录'),
            trailing: const Icon(Icons.chevron_right),
            onTap: _showChapterList,
          ),
        ],
      ),
    );
  }

  void _changeFontSize(double delta) {
    setState(() {
      _fontSize = (_fontSize + delta).clamp(12, 32);
    });
  }

  void _showChapterList() {
    showModalBottomSheet(
      context: context,
      builder: (context) => _buildChapterList(),
    );
  }

  Widget _buildChapterList() {
    return ListView.builder(
      itemCount: _currentBook.chapters.length,
      itemBuilder: (context, index) {
        final chapter = _currentBook.chapters[index];
        return ListTile(
          title: Text(chapter.title),
          selected: chapter.index == _currentChapter.index,
          onTap: () async {
            Navigator.pop(context);
            setState(() {
              _currentChapter = chapter;
              _content = '';
              _pageContents.clear();
            });
            await _loadChapterContent();
            _pageController = PageController();
            _currentPage = 0;
            _updateReadingProgress();
          },
        );
      },
    );
  }

  @override
  void dispose() {
    _pageController.removeListener(_handlePageChanged);
    _pageController.dispose();
    super.dispose();
  }
}
