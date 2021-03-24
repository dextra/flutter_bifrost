import 'package:flutter/widgets.dart';

import 'bifrost_manager.dart';

class Bifrost {
  const Bifrost._();

  static TransitionBuilder init({TransitionBuilder builder}) {
    return (BuildContext context, Widget child) {
      assert(child is Navigator, 'child must be Navigator, what is wrong?');

      final manager = BifrostManager(child as Navigator);
      final content = HeroControllerScope.none(child: manager);

      if (builder != null) {
        return builder(context, content);
      } else {
        return content;
      }
    };
  }
}
