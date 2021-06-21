import 'package:bifrost/bifrost.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class SecondPage extends StatelessWidget {
  final focusNode = FocusNode();
  final controller = TextEditingController();

  @override
  Widget build(BuildContext context) {
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: SystemUiOverlayStyle.dark,
      child: Scaffold(
        body: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Second Flutter Page',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.headline6,
              ),
              SizedBox(height: 16),
              TextField(
                controller: controller,
                focusNode: focusNode,
                textCapitalization: TextCapitalization.sentences,
                decoration: InputDecoration(
                  border: OutlineInputBorder(),
                  hintText: 'Type your name',
                ),
              ),
              SizedBox(height: 16),
              AnimatedBuilder(
                animation: controller,
                builder: (context, widget) {
                  final enableButton = controller.text.isNotEmpty;
                  return ElevatedButton(
                    child: Text('Show Greetings'),
                    style: ElevatedButton.styleFrom(
                      padding: EdgeInsets.symmetric(vertical: 20),
                      primary: Colors.deepPurpleAccent,
                      textStyle: TextStyle(
                        color: Colors.white,
                      ),
                    ),
                    onPressed: enableButton ? openGreetingsPage : null,
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  void openGreetingsPage() {
    focusNode.unfocus();
    BifrostChannels.notification
        .invokeMethod('openGreetingsPage', controller.text);
  }
}
