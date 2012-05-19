Introduction
============

InstaCam, Instagram alike application for realtime photographing manipulation. Unlike Instagram,
InstaCam allows user to adjust color filters in realtime with use of OpenGL ES 2.0 external
textures. This allows camera preview to be rendered on screen using regular OpenGL ES 2.0
GLSL shaders and in return make it possible to adjust image in realtime.

Unlike most of the other applications this one will not end up to the Play Market soonish.
Mainly for reason I'm using API level 15+ requirement and the whole project is more of
an exercise for beefing one's knowledge on OpenGL external OES textures and RenderScript.
Latter one is supposed to be used for final image constitution eventually.

License
=======

Application icon used within .APK installation file and on Google Play -market, is taken
from icon pack by [Yanko Andreev](http://yankoa.deviantart.com/). From his MetroDroid
icon pack to be more precise - which is released under
[Creative Commons -license](http://creativecommons.org/licenses/by-nc-nd/3.0/) and
should be legal for usage under non-commercial applications.

Other icons used in the application are taken from Faience icon theme made by
[Matthieu James](http://tiheum.deviantart.com/). These icons are licensed under
GPLv3 license and it should be sufficient to provide my slightly modified versions
here among with other source code..

Some code is translated from [LightBox](https://github.com/lightbox/PhotoProcessing)
Photo Processing project into GLSL and RenderScript code. For filters that is. It's
highly recommended to take a look on this project if you're into familiarizing
yourself with color filters.

Beyond these exceptions, all code what-so-ever textual content, including shaders, layouts etc,
is licensed under Apache 2.0 License (http://www.apache.org/licenses/LICENSE-2.0.html) and can be used in commercial or personal projects.
