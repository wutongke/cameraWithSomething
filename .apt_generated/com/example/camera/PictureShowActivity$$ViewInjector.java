// Generated code from Butter Knife. Do not modify!
package com.example.camera;

import android.view.View;
import butterknife.ButterKnife.Finder;

public class PictureShowActivity$$ViewInjector {
  public static void inject(Finder finder, final com.example.camera.PictureShowActivity target, Object source) {
    View view;
    view = finder.findRequiredView(source, 2131296259, "field 'myGridView'");
    target.myGridView = (android.widget.GridView) view;
  }

  public static void reset(com.example.camera.PictureShowActivity target) {
    target.myGridView = null;
  }
}
