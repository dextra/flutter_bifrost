import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class GreetingsPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final settings = ModalRoute.of(context)?.settings;
    final containerType = Platform.isIOS ? 'ViewController' : 'Activity';

    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark,
      child: Scaffold(
        appBar: AppBar(
          title: Text('Greetings Page'),
          leading: BackButton(),
        ),
        body: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Hello, ${settings?.arguments}!',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headline6,
              ),
              SizedBox(height: 16),
              Text(
                'This is a new BifrostFlutter$containerType',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.subtitle1,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
