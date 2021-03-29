import 'package:bifrost/bifrost.dart';
import 'package:flutter/material.dart';

import 'pages/first_page.dart';
import 'pages/greetings_page.dart';
import 'pages/second_page.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      builder: Bifrost.init(),
      routes: routes,
      theme: theme,
    );
  }

  Map<String, WidgetBuilder> get routes => {
        '/first': (context) => FirstPage(),
        '/second': (context) => SecondPage(),
        '/greetings': (context) => GreetingsPage(),
      };

  ThemeData get theme => ThemeData(
        primaryColor: Colors.deepPurpleAccent,
        accentColor: Colors.deepPurpleAccent,
        appBarTheme: AppBarTheme(
          color: Colors.deepPurpleAccent,
        ),
      );
}
