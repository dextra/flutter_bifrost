import 'dart:developer' as developer;

class Level {
  final String name;

  /// Unique value for this level. Used to order levels, so filtering can
  /// exclude messages whose level is under certain value.
  final int value;

  const Level(this.name, this.value);

  /// Key for static configuration messages ([value] = 700).
  static const Level CONFIG = Level('CONFIG', 700);

  /// Key for informational messages ([value] = 800).
  static const Level INFO = Level('INFO', 800);

  /// Key for potential problems ([value] = 900).
  static const Level WARNING = Level('WARNING', 900);
}

void log(String message,
    {Level level = Level.CONFIG, String name = 'bifrost'}) {
  assert(() {
    developer.log(
      message,
      level: level.value,
      name: name,
      time: DateTime.now(),
    );
    return true;
  }());
}
