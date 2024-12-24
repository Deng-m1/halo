class Chapter {
  final int index;
  final String title;
  final int startPosition;
  final int endPosition;

  Chapter({
    required this.index,
    required this.title,
    required this.startPosition,
    required this.endPosition,
  });

  factory Chapter.fromJson(Map<String, dynamic> json) {
    return Chapter(
      index: json['index'] as int,
      title: json['title'] as String,
      startPosition: json['startPosition'] as int,
      endPosition: json['endPosition'] as int,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'index': index,
      'title': title,
      'startPosition': startPosition,
      'endPosition': endPosition,
    };
  }

  Chapter copyWith({
    int? index,
    String? title,
    int? startPosition,
    int? endPosition,
  }) {
    return Chapter(
      index: index ?? this.index,
      title: title ?? this.title,
      startPosition: startPosition ?? this.startPosition,
      endPosition: endPosition ?? this.endPosition,
    );
  }
}
